package io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions

import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionClaimStatus
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsWithTotalAmount
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.claimStatusOf
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic.claimContribution
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimatorFlow
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ScreenScope
class ClaimContributionsInteractor @Inject constructor(
    private val accountRepository: AccountRepository,
    private val crowdloanState: CrowdloanSharedState,
    private val chainStateRepository: ChainStateRepository,
    private val contributionsRepository: ContributionsRepository,
    private val extrinsicService: ExtrinsicService,
) {

    fun claimableContributions(): Flow<ContributionsWithTotalAmount<Contribution>> {
        return flowOfAll {
            val account = accountRepository.getSelectedMetaAccount()
            val (chain, asset) = crowdloanState.chainAndAsset()

            combine(
                chainStateRepository.blockDurationEstimatorFlow(chain.timelineChainIdOrSelf()),
                contributionsRepository.observeContributions(account, chain, asset)
            ) { blockDurationEstimator, contributions ->
                contributions.filter {
                    val claimStatus = blockDurationEstimator.claimStatusOf(it)
                    claimStatus is ContributionClaimStatus.Claimable
                }
            }
                .map { claimableContributions -> ContributionsWithTotalAmount.fromContributions(claimableContributions) }
        }
    }

    suspend fun estimateFee(contributions: List<Contribution>): SubmissionFee {
        val chain = crowdloanState.chain()

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) { context ->
            val depositor = context.submissionOrigin.executingAccount
            claim(contributions, depositor)
        }
    }

    suspend fun claim(contributions: List<Contribution>): Result<ExtrinsicExecutionResult> {
        val chain = crowdloanState.chain()

        return extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.SelectedWallet) { context ->
            val depositor = context.submissionOrigin.executingAccount
            claim(contributions, depositor)
        }
            .requireOk()
    }

    private fun ExtrinsicBuilder.claim(contributions: List<Contribution>, depositor: AccountId) {
        contributions.forEach { contribution ->
            claimContribution(contribution.paraId, contribution.unlockBlock, depositor)
        }
    }
}
