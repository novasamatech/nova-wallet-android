package io.novafoundation.nova.common.view

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewInputFieldBinding
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.getCornersStateDrawable
import io.novafoundation.nova.common.view.shape.getInputBackground
import kotlin.math.roundToInt

enum class BackgroundMode {
    INPUT_STATE,
    SOLID,
    CUSTOM,
}

class InputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.style.Widget_Nova_Input_Primary_External,
) : TextInputLayout(context, attrs, defStyle) {

    private val binder = ViewInputFieldBinding.inflate(inflater(), this)

    val content: EditText
        get() = editText!!

    init {
        content.setHintTextColor(context.getColor(R.color.hint_text))
        content.background = context.getCornersStateDrawable()

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attributeSet: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.InputField)

        val inputType = typedArray.getInt(R.styleable.InputField_android_inputType, InputType.TYPE_CLASS_TEXT)
        content.inputType = inputType

        val text = typedArray.getString(R.styleable.InputField_android_text)
        content.setText(text)

        content.setPadding(
            typedArray.getDimension(R.styleable.InputField_editTextPaddingStart, content.paddingLeft.toFloat()).roundToInt(),
            typedArray.getDimension(R.styleable.InputField_editTextPaddingTop, content.paddingTop.toFloat()).roundToInt(),
            typedArray.getDimension(R.styleable.InputField_editTextPaddingEnd, content.paddingRight.toFloat()).roundToInt(),
            typedArray.getDimension(R.styleable.InputField_editTextPaddingBottom, content.paddingBottom.toFloat()).roundToInt()
        )

        val contentHint = typedArray.getString(R.styleable.InputField_editTextHint)
        if (contentHint != null) {
            hint = null
            content.hint = contentHint
        }

        val hintColor = typedArray.getColor(R.styleable.InputField_editTextHintColor, context.getColor(R.color.hint_text))
        content.setHintTextColor(hintColor)

        val backgroundMode = typedArray.getEnum(R.styleable.InputField_backgroundMode, BackgroundMode.INPUT_STATE)
        when (backgroundMode) {
            BackgroundMode.INPUT_STATE -> content.background = context.getCornersStateDrawable()
            BackgroundMode.SOLID -> content.background = context.getInputBackground()
            BackgroundMode.CUSTOM -> {}
        }

        val textAppearanceRes = typedArray.getResourceIdOrNull(R.styleable.InputField_android_textAppearance)
        if (textAppearanceRes != null) {
            content.setTextAppearance(textAppearanceRes)
        }

        typedArray.recycle()
    }
}
