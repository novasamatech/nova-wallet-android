package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala

import io.novafoundation.nova.common.base.BaseException
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaDirectContributeRequest
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaLiquidContributeRequest
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.extrinsic.systemRemarkWithEvent
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.state.chainAsset
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.fromUtf8
import java.math.BigDecimal

class AcalaContributeInteractor(
    private val acalaApi: AcalaApi,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val selectedAssetState: SingleAssetSharedState,
    private val signerProvider: SignerProvider,
) {

    suspend fun registerContributionOffChain(
        amount: BigDecimal,
        contributionType: ContributionType,
        referralCode: String?,
    ): Result<Unit> = runCatching {
        httpExceptionHandler.wrap {
            val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
            val signer = signerProvider.rootSignerFor(selectedMetaAccount)

            val (chain, chainAsset) = selectedAssetState.chainAndAsset()

            val accountIdInCurrentChain = selectedMetaAccount.accountIdIn(chain)!!
            // api requires polkadot address even in rococo testnet
            val addressInPolkadot = chainRegistry.getChain(ChainGeneses.POLKADOT).addressOf(accountIdInCurrentChain)
            val amountInPlanks = chainAsset.planksFromAmount(amount)

            val statement = getStatement(chain).statement

            val signerPayload = SignerPayloadRaw.fromUtf8(statement, accountIdInCurrentChain)

            when (contributionType) {
                ContributionType.DIRECT -> {
                    val request = AcalaDirectContributeRequest(
                        address = addressInPolkadot,
                        amount = amountInPlanks,
                        referral = referralCode,
                        signature = signer.signRaw(signerPayload).asHexString()
                    )

                    acalaApi.directContribute(
                        baseUrl = AcalaApi.getBaseUrl(chain),
                        authHeader = AcalaApi.getAuthHeader(chain),
                        body = request
                    )
                }

                ContributionType.LIQUID -> {
                    val request = AcalaLiquidContributeRequest(
                        address = addressInPolkadot,
                        amount = amountInPlanks,
                        referral = referralCode
                    )

                    acalaApi.liquidContribute(
                        baseUrl = AcalaApi.getBaseUrl(chain),
                        authHeader = AcalaApi.getAuthHeader(chain),
                        body = request
                    )
                }
            }
        }
    }

    suspend fun isReferralValid(referralCode: String) = try {
        val chain = selectedAssetState.chain()

        httpExceptionHandler.wrap {
            acalaApi.isReferralValid(
                baseUrl = AcalaApi.getBaseUrl(chain),
                authHeader = AcalaApi.getAuthHeader(chain),
                referral = referralCode
            ).result
        }
    } catch (e: BaseException) {
        if (e.kind == BaseException.Kind.HTTP) {
            false // acala api return an error http code for some invalid codes, so catch it here
        } else {
            throw e
        }
    }

    suspend fun injectOnChainSubmission(
        contributionType: ContributionType,
        referralCode: String?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) = with(extrinsicBuilder) {
        if (contributionType == ContributionType.LIQUID) {
            resetCalls()

            val (chain, chainAsset) = selectedAssetState.chainAndAsset()
            val amountInPlanks = chainAsset.planksFromAmount(amount)

            val statement = httpExceptionHandler.wrap { getStatement(chain) }
            val proxyAccountId = chain.accountIdOf(statement.proxyAddress)

            nativeTransfer(proxyAccountId, amountInPlanks)
            systemRemarkWithEvent(statement.statement)
            referralCode?.let { systemRemarkWithEvent(referralRemark(it)) }
        }
    }

    suspend fun injectFeeCalculation(
        contributionType: ContributionType,
        referralCode: String?,
        amount: BigDecimal,
        extrinsicBuilder: ExtrinsicBuilder,
    ) = with(extrinsicBuilder) {
        if (contributionType == ContributionType.LIQUID) {
            resetCalls()

            val chainAsset = selectedAssetState.chainAsset()
            val amountInPlanks = chainAsset.planksFromAmount(amount)

            val fakeDestination = ByteArray(32)
            nativeTransfer(accountId = fakeDestination, amount = amountInPlanks)

            val fakeAgreementRemark = ByteArray(185) // acala agreement is 185 bytes
            systemRemarkWithEvent(fakeAgreementRemark)

            referralCode?.let { systemRemarkWithEvent(referralRemark(referralCode)) }
        }
    }

    private suspend fun getStatement(
        chain: Chain,
    ) = acalaApi.getStatement(
        baseUrl = AcalaApi.getBaseUrl(chain),
        authHeader = AcalaApi.getAuthHeader(chain)
    )

    private fun referralRemark(referralCode: String) = "referrer:$referralCode"
}
