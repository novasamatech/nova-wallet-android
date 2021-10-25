package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.hasChainAccountIn
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.MoonbeamApi
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.extrinsic.systemRemark
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class MoonbeamCrowdloanInteractor(
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService,
    private val moonbeamApi: MoonbeamApi,
    private val selectedChainAssetState: SingleAssetSharedState,
    private val httpExceptionHandler: HttpExceptionHandler,
) {

    fun getTermsLink() = "https://github.com/moonbeam-foundation/crowdloan-self-attestation/blob/main/moonbeam/README.md"

    suspend fun flowStatus(): MoonbeamFlowStatus {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val moonbeamChainId = Chain.Geneses.MOONBEAM

        return when {
            !metaAccount.hasChainAccountIn(moonbeamChainId) -> MoonbeamFlowStatus.NeedsChainAccount(
                chainId = moonbeamChainId,
                metaId = metaAccount.id
            )
            // TODO other statuses
            else -> MoonbeamFlowStatus.ReadyToComplete
        }
    }

    suspend fun calculateTermsFee(): BigInteger = withContext(Dispatchers.Default) {
        val chain = selectedChainAssetState.chain()

        extrinsicService.estimateFee(chain) {
            systemRemark(fakeRemark())
        }
    }

    suspend fun submitTerms(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val chain = selectedChainAssetState.chain()

            val legalText = httpExceptionHandler.wrap { moonbeamApi.getLegalText() }

            TODO("TODO submit remark onchain, wait, submit proofs to api")
        }
    }

    private fun fakeRemark() = ByteArray(32)
}
