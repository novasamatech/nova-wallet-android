package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam

import android.util.Log
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.sha256
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.cryptoTypeIn
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.AgreeRemarkRequest
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.MoonbeamApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.VerifyRemarkRequest
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.agreeRemark
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.checkRemark
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.moonbeamChainId
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.verifyRemark
import io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic.addMemo
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.Crowdloan
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.systemRemark
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.fromUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class VerificationError : Exception()

private val SUPPORTED_CRYPTO_TYPES = setOf(CryptoType.SR25519, CryptoType.ED25519)

class MoonbeamCrowdloanInteractor(
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService,
    private val moonbeamApi: MoonbeamApi,
    private val selectedChainAssetState: SingleAssetSharedState,
    private val chainRegistry: ChainRegistry,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val signerProvider: SignerProvider,
) {

    fun getTermsLink() = "https://github.com/moonbeam-foundation/crowdloan-self-attestation/blob/main/moonbeam/README.md"

    suspend fun getMoonbeamRewardDestination(parachainMetadata: ParachainMetadata): CrossChainRewardDestination {
        val currentAccount = accountRepository.getSelectedMetaAccount()
        val moonbeamChain = chainRegistry.getChain(parachainMetadata.moonbeamChainId())

        return CrossChainRewardDestination(
            addressInDestination = currentAccount.addressIn(moonbeamChain)!!,
            destination = moonbeamChain
        )
    }

    suspend fun additionalSubmission(
        crowdloan: Crowdloan,
        extrinsicBuilder: ExtrinsicBuilder,
    ) {
        val rewardDestination = getMoonbeamRewardDestination(crowdloan.parachainMetadata!!)

        extrinsicBuilder.addMemo(
            parachainId = crowdloan.parachainId,
            memo = rewardDestination.addressInDestination.fromHex()
        )
    }

    suspend fun flowStatus(parachainMetadata: ParachainMetadata): Result<MoonbeamFlowStatus> = withContext(Dispatchers.Default) {
        runCatching {
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val moonbeamChainId = parachainMetadata.moonbeamChainId()
            val moonbeamChain = chainRegistry.getChain(moonbeamChainId)

            val currentChain = selectedChainAssetState.chain()
            val currentAddress = metaAccount.addressIn(currentChain)!!

            when {
                !metaAccount.hasAccountIn(moonbeamChain) -> MoonbeamFlowStatus.NeedsChainAccount(
                    chainId = moonbeamChainId,
                    metaId = metaAccount.id
                )

                metaAccount.cryptoTypeIn(currentChain) !in SUPPORTED_CRYPTO_TYPES -> MoonbeamFlowStatus.UnsupportedAccountEncryption

                else -> when (checkRemark(parachainMetadata, currentAddress)) {
                    null -> MoonbeamFlowStatus.RegionNotSupported
                    true -> MoonbeamFlowStatus.Completed
                    false -> MoonbeamFlowStatus.ReadyToComplete
                }
            }
        }
    }

    suspend fun calculateTermsFee(): Fee = withContext(Dispatchers.Default) {
        val chain = selectedChainAssetState.chain()

        extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            systemRemark(fakeRemark())
        }
    }

    suspend fun submitAgreement(parachainMetadata: ParachainMetadata): Result<*> = withContext(Dispatchers.Default) {
        runCatching {
            val chain = selectedChainAssetState.chain()
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val currentAddress = metaAccount.addressIn(chain)!!
            val accountId = metaAccount.accountIdIn(chain)!!

            val legalText = httpExceptionHandler.wrap { moonbeamApi.getLegalText() }
            val legalHash = legalText.encodeToByteArray().sha256().toHexString(withPrefix = false)

            val signer = signerProvider.rootSignerFor(metaAccount)
            val signerPayload = SignerPayloadRaw.fromUtf8(legalHash, accountId)

            val signedHash = signer.signRaw(signerPayload).asHexString()

            val agreeRemarkRequest = AgreeRemarkRequest(currentAddress, signedHash)
            val remark = httpExceptionHandler.wrap { moonbeamApi.agreeRemark(parachainMetadata, agreeRemarkRequest) }.remark

            val finalizedStatus = extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                systemRemark(remark.encodeToByteArray())
            }
                .getOrThrow()
                .filterIsInstance<ExtrinsicStatus.Finalized>()
                .first()

            Log.d(this@MoonbeamCrowdloanInteractor.LOG_TAG, "Finalized ${finalizedStatus.extrinsicHash} in block ${finalizedStatus.blockHash}")

            val verificationRequest = VerifyRemarkRequest(
                address = currentAddress,
                extrinsicHash = finalizedStatus.extrinsicHash,
                blockHash = finalizedStatus.blockHash
            )
            val verificationResponse = httpExceptionHandler.wrap { moonbeamApi.verifyRemark(parachainMetadata, verificationRequest) }

            if (!verificationResponse.verified) throw VerificationError()
        }
    }

    private fun fakeRemark() = ByteArray(32)

    /**
     * @return null if Geo-fenced or application unavailable. True if user already agreed with terms. False otherwise
     */
    private suspend fun checkRemark(parachainMetadata: ParachainMetadata, address: String): Boolean? = try {
        moonbeamApi.checkRemark(parachainMetadata, address).verified
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
