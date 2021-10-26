package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala

import io.novafoundation.nova.common.base.BaseException
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.feature_account_api.data.secrets.sign
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.karura.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.karura.VerifyKaruraParticipationRequest
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import java.math.BigDecimal

class AcalaContributeInteractor(
    private val acalaApi: AcalaApi,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
    private val selectedAssetState: SingleAssetSharedState,
) {

    suspend fun registerInBonusProgram(referralCode: String, amount: BigDecimal): Result<Unit> = runCatching {
        httpExceptionHandler.wrap {
            val selectedMetaAccount = accountRepository.getSelectedMetaAccount()

            val (chain, chainAsset) = selectedAssetState.chainAndAsset()

            val statement = acalaApi.getStatement(AcalaApi.getBaseUrl(chain)).statement

            val chainForAddress = when (chain.genesisHash) {
                ChainGeneses.ROCOCO_ACALA -> chainRegistry.getChain(ChainGeneses.POLKADOT) // api requires polkadot address even in rococo testnet
                else -> chain
            }

            val request = VerifyKaruraParticipationRequest(
                address = selectedMetaAccount.addressIn(chainForAddress)!!,
                amount = chainAsset.planksFromAmount(amount),
                referral = referralCode,
                signature = secretStoreV2.sign(selectedMetaAccount, chain, statement)
            )

            acalaApi.applyForBonus(AcalaApi.getBaseUrl(chain), request)
        }
    }

    suspend fun isReferralValid(referralCode: String) = try {
        val chain = selectedAssetState.chain()

        httpExceptionHandler.wrap {
            acalaApi.isReferralValid(AcalaApi.getBaseUrl(chain), referralCode).result
        }
    } catch (e: BaseException) {
        if (e.kind == BaseException.Kind.HTTP) {
            false // acala api return an error http code for some invalid codes, so catch it here
        } else {
            throw e
        }
    }
}
