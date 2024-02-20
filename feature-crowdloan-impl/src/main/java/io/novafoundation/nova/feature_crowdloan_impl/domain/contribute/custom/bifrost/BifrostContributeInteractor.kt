package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.bifrost

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.bifrost.BifrostApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.bifrost.getAccountByReferralCode
import io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.extrinsic.addMemo
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

class BifrostContributeInteractor(
    val novaReferralCode: String,
    private val bifrostApi: BifrostApi,
    private val httpExceptionHandler: HttpExceptionHandler,
) {

    suspend fun isCodeValid(code: String): Boolean {
        val response = httpExceptionHandler.wrap { bifrostApi.getAccountByReferralCode(code) }

        return response.data.getAccountByInvitationCode.account.isNullOrEmpty().not()
    }

    fun submitOnChain(
        paraId: ParaId,
        referralCode: String,
        extrinsicBuilder: ExtrinsicBuilder,
    ) = extrinsicBuilder.addMemo(paraId, referralCode)
}
