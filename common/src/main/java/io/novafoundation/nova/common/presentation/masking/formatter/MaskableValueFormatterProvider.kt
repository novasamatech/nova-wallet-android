package io.novafoundation.nova.common.presentation.masking.formatter

import io.novafoundation.nova.common.domain.usecase.MaskingModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MaskableValueFormatterProvider(
    private val maskableValueFormatterFactory: MaskableValueFormatterFactory,
    private val maskingModeUseCase: MaskingModeUseCase
) {

    fun provideFormatter(): Flow<MaskableValueFormatter> {
        return maskingModeUseCase.observeMaskingMode().map {
            maskableValueFormatterFactory.create(it)
        }
    }
}
