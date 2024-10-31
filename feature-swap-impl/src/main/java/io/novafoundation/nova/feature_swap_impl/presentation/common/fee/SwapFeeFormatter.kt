package io.novafoundation.nova.feature_swap_impl.presentation.common.fee

import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus

class SwapFeeFormatter(
    private val swapInteractor: SwapInteractor,
) : FeeFormatter<SwapFee, FeeDisplay> {

    override suspend fun formatFee(
        fee: SwapFee,
        configuration: FeeFormatter.Configuration,
        context: FeeFormatter.Context
    ): FeeDisplay {
        val totalFiatFee = swapInteractor.calculateTotalFiatPrice(fee)
        val formattedFiatFee = totalFiatFee.price.formatAsCurrency(totalFiatFee.currency)
        return FeeDisplay(
            title = formattedFiatFee,
            subtitle = null
        )
    }

    override suspend fun createLoadingStatus(): FeeStatus.Loading {
        return FeeStatus.Loading(visibleDuringProgress = true)
    }
}
