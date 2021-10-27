package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.data.repository.hasWonAuction
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic.contribute
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.PrivateCrowdloanSignatureProvider.Mode
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.chainAndAsset
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

typealias OnChainSubmission = suspend ExtrinsicBuilder.() -> Unit

class CrowdloanContributeInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
    private val chainStateRepository: ChainStateRepository,
    private val customContributeManager: CustomContributeManager,
    private val crowdloanSharedState: CrowdloanSharedState,
    private val crowdloanRepository: CrowdloanRepository,
) {

    fun crowdloanStateFlow(
        parachainId: ParaId,
        parachainMetadata: ParachainMetadata?,
    ): Flow<Crowdloan> = crowdloanSharedState.assetWithChain.flatMapLatest { (chain, _) ->
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = selectedMetaAccount.accountIdIn(chain)!! // TODO optional for ethereum chains

        val expectedBlockTime = chainStateRepository.expectedBlockTimeInMillis(chain.id)
        val blocksPerLeasePeriod = crowdloanRepository.blocksPerLeasePeriod(chain.id)

        combine(
            crowdloanRepository.fundInfoFlow(chain.id, parachainId),
            chainStateRepository.currentBlockNumberFlow(chain.id)
        ) { fundInfo, blockNumber ->
            val contribution = crowdloanRepository.getContribution(chain.id, accountId, parachainId, fundInfo.trieIndex)
            val hasWonAuction = crowdloanRepository.hasWonAuction(chain.id, fundInfo)

            mapFundInfoToCrowdloan(
                fundInfo = fundInfo,
                parachainMetadata = parachainMetadata,
                parachainId = parachainId,
                currentBlockNumber = blockNumber,
                expectedBlockTimeInMillis = expectedBlockTime,
                blocksPerLeasePeriod = blocksPerLeasePeriod,
                contribution = contribution,
                hasWonAuction = hasWonAuction
            )
        }
    }

    suspend fun estimateFee(
        crowdloan: Crowdloan,
        contribution: BigDecimal,
        additional: OnChainSubmission?,
    ) = formingSubmission(crowdloan, contribution, additional, toCalculateFee = true) { submission, chain, _ ->
        extrinsicService.estimateFee(chain, submission)
    }

    suspend fun contribute(
        crowdloan: Crowdloan,
        contribution: BigDecimal,
        additional: OnChainSubmission?,
    ) = formingSubmission(crowdloan, contribution, additional, toCalculateFee = false) { submission, chain, account ->
        val accountId = account.accountIdIn(chain)!!

        extrinsicService.submitExtrinsic(chain, accountId, submission)
    }.getOrThrow()

    private suspend fun <T> formingSubmission(
        crowdloan: Crowdloan,
        contribution: BigDecimal,
        additional: OnChainSubmission?,
        toCalculateFee: Boolean,
        finalAction: suspend (OnChainSubmission, Chain, MetaAccount) -> T,
    ): T = withContext(Dispatchers.Default) {
        val (chain, chainAsset) = crowdloanSharedState.chainAndAsset()
        val contributionInPlanks = chainAsset.planksFromAmount(contribution)
        val account = accountRepository.getSelectedMetaAccount()

        val privateSignature = crowdloan.parachainMetadata?.customFlow?.let {
            val previousContribution = crowdloan.myContribution?.amount ?: BigInteger.ZERO

            val signatureProvider = customContributeManager.getFactoryOrNull(it)?.privateCrowdloanSignatureProvider
            val address = account.addressIn(chain)!!

            signatureProvider?.provideSignature(
                chainMetadata = crowdloan.parachainMetadata,
                previousContribution = previousContribution,
                newContribution = contributionInPlanks,
                address = address,
                mode = if (toCalculateFee) Mode.FEE else Mode.SUBMIT
            )
        }

        val submission: OnChainSubmission = {
            contribute(crowdloan.parachainId, contributionInPlanks, privateSignature)

            additional?.invoke(this)
        }

        finalAction(submission, chain, account)
    }
}
