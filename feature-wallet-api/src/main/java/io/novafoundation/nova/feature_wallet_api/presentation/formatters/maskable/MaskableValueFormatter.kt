package io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable

import io.novafoundation.nova.common.data.model.MaskingMode
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.toMaskableModel

interface MaskableValueFormatter {

    fun <T> format(valueReceiver: () -> T): MaskableModel<T>
}

class MaskableValueFormatterFactory {
    fun create(maskingMode: MaskingMode): MaskableValueFormatter {
        return RealMaskableValueFormatter(maskingMode)
    }
}

private class RealMaskableValueFormatter(private val maskingMode: MaskingMode) : MaskableValueFormatter {

    override fun <T> format(valueReceiver: () -> T): MaskableModel<T> {
        return maskingMode.toMaskableModel(valueReceiver)
    }
}
