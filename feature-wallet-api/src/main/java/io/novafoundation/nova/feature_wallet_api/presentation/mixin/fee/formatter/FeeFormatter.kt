package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter.Context
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus

interface FeeFormatter<F, D> {

    class Configuration(
        val showZeroFiat: Boolean
    )

    interface Context {

        suspend fun feeToken(): Token
    }

    suspend fun formatFee(
        fee: F,
        configuration: Configuration,
        context: Context,
    ): D

    suspend fun createLoadingStatus(): FeeStatus.Loading
}

context(Context)
suspend fun <F, D> FeeFormatter<F, D>.formatFeeStatus(
    fee: F?,
    configuration: FeeFormatter.Configuration,
): FeeStatus<F, D> {
    return if (fee != null) {
        val display = formatFee(fee, configuration, this@Context)
        val feeModel = FeeModel(fee, display)
        FeeStatus.Loaded(feeModel)
    } else {
        FeeStatus.NoFee
    }
}
