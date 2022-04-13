package io.novafoundation.nova.common.view.input

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.view.shape.getInputBackground

class TextInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.defaultTextInputStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        background = context.getInputBackground()
    }
}
