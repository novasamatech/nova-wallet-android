package io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable

import io.novafoundation.nova.common.domain.interactor.DiscreetModeInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MaskableValueFormatterProvider(
    private val maskableValueFormatterFactory: MaskableValueFormatterFactory,
    private val discreetModeInteractor: DiscreetModeInteractor
) {

    fun provideFormatter(): Flow<MaskableValueFormatter> {
        return discreetModeInteractor.observeDiscreetMode().map {
            maskableValueFormatterFactory.create(it)
        }
    }
}
