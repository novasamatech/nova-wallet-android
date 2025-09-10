package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.maskable

import io.novafoundation.nova.common.domain.interactor.DiscreetModeInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MaskableAmountFormatterProvider(
    private val maskableAmountFormatterFactory: MaskableAmountFormatterFactory,
    private val discreetModeInteractor: DiscreetModeInteractor
) {

    fun provideFormatter(): Flow<MaskableAmountFormatter> {
        return discreetModeInteractor.observeDiscreetMode().map {
            maskableAmountFormatterFactory.create(it)
        }
    }
}
