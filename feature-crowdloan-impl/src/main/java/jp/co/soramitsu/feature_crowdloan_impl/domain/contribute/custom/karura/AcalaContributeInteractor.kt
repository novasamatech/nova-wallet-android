package jp.co.soramitsu.feature_crowdloan_impl.domain.contribute.custom.karura

import jp.co.soramitsu.common.base.BaseException
import jp.co.soramitsu.common.data.network.HttpExceptionHandler
import jp.co.soramitsu.common.data.secrets.v2.SecretStoreV2
import jp.co.soramitsu.feature_account_api.data.secrets.sign
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_api.domain.model.addressIn
import jp.co.soramitsu.feature_crowdloan_impl.data.network.api.karura.AcalaApi
import jp.co.soramitsu.feature_crowdloan_impl.data.network.api.karura.VerifyKaruraParticipationRequest
import jp.co.soramitsu.feature_wallet_api.domain.model.planksFromAmount
import jp.co.soramitsu.runtime.state.SingleAssetSharedState
import jp.co.soramitsu.runtime.state.chain
import jp.co.soramitsu.runtime.state.chainAndAsset
import java.math.BigDecimal

class AcalaContributeInteractor(
    private val acalaApi: AcalaApi,
    private val httpExceptionHandler: HttpExceptionHandler,
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val selectedAssetState: SingleAssetSharedState,
) {

    suspend fun registerInBonusProgram(referralCode: String, amount: BigDecimal): Result<Unit> = runCatching {
        httpExceptionHandler.wrap {
            val selectedMetaAccount = accountRepository.getSelectedMetaAccount()

            val (chain, chainAsset) = selectedAssetState.chainAndAsset()

            val statement = acalaApi.getStatement(AcalaApi.getBaseUrl(chain)).statement

            val request = VerifyKaruraParticipationRequest(
                address = selectedMetaAccount.addressIn(chain)!!,
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
