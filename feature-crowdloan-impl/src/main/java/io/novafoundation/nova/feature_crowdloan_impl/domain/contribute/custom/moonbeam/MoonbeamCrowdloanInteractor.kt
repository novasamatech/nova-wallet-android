package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.sha256
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.secrets.sign
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.hasChainAccountIn
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.AgreeRemarkRequest
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.MoonbeamApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.agreeRemark
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.checkRemark
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.extrinsic.systemRemark
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.math.BigInteger

class MoonbeamCrowdloanInteractor(
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService,
    private val moonbeamApi: MoonbeamApi,
    private val selectedChainAssetState: SingleAssetSharedState,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val secretStoreV2: SecretStoreV2,
) {

    fun getTermsLink() = "https://github.com/moonbeam-foundation/crowdloan-self-attestation/blob/main/moonbeam/README.md"

    suspend fun flowStatus(): Result<MoonbeamFlowStatus> = withContext(Dispatchers.Default) {
        runCatching {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val moonbeamChainId = Chain.Geneses.MOONBEAM
            val currentChain = selectedChainAssetState.chain()
            val currentAddress = metaAccount.addressIn(currentChain)!!

            when {
                !metaAccount.hasChainAccountIn(moonbeamChainId) -> MoonbeamFlowStatus.NeedsChainAccount(
                    chainId = moonbeamChainId,
                    metaId = metaAccount.id
                )
                else -> when (checkRemark(currentChain, currentAddress)) {
                    null -> MoonbeamFlowStatus.RegionNotSupported
                    true -> MoonbeamFlowStatus.Completed
                    false -> MoonbeamFlowStatus.ReadyToComplete
                }
            }
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
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val currentAddress = metaAccount.addressIn(chain)!!

            val legalText = httpExceptionHandler.wrap { moonbeamApi.getLegalText() }
            val legalHash = legalText.encodeToByteArray().sha256().toHexString(withPrefix = false)
            val signedHash = secretStoreV2.sign(metaAccount, chain, legalHash)

            val agreeRemarkRequest = AgreeRemarkRequest(currentAddress, signedHash)
            val remark = httpExceptionHandler.wrap { moonbeamApi.agreeRemark(chain, agreeRemarkRequest) }.remark
        }
    }

    private fun fakeRemark() = ByteArray(32)

    /**
     * @return null if Geo-fenced or application unavailable. True if user already agreed with terms. False otherwise
     */
    private suspend fun checkRemark(chain: Chain, address: String): Boolean? = try {
        moonbeamApi.checkRemark(chain, address).verified
    } catch (e: HttpException) {
        if (e.code() == 403) { // Moonbeam answers with 403 in case geo-fenced or application unavailable
            null
        } else {
            throw httpExceptionHandler.transformException(e)
        }
    } catch (e: Exception) {
        throw httpExceptionHandler.transformException(e)
    }
}
