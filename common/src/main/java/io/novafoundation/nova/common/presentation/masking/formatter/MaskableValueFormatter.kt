package io.novafoundation.nova.common.presentation.masking.formatter

import io.novafoundation.nova.common.data.model.MaskingMode
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.toMaskableModel

interface MaskableValueFormatter {

    // TODO: valueReceiver should be suspend to support suspendable logic
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
