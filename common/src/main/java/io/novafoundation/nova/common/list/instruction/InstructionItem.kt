package io.novafoundation.nova.common.list.instruction

import androidx.annotation.DrawableRes

sealed interface InstructionItem {
    class Step(
        val number: Int,
        val text: CharSequence
    ) : InstructionItem

    class Image(
        @DrawableRes val image: Int,
        val label: String?
    ) : InstructionItem
}
