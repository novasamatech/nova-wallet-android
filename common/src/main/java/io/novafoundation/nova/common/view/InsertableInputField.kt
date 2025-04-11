package io.novafoundation.nova.common.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewInsertableInputFieldBinding
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

class InsertableInputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewInsertableInputFieldBinding.inflate(inflater(), this)

    private var clipboardManager: ClipboardManager? = getClipboardManager()

    val content: EditText
        get() = binder.actionInputField

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        setAddStatesFromChildren(true)

        setBackgrounds()

        attrs?.let(::applyAttributes)

        content.addTextChangedListener {
            updateButtonsVisibility(it)
        }

        binder.actionInputFieldAction.setOnClickListener {
            paste()
        }

        binder.actionInputFieldClear.setOnClickListener { content.text = null }

        updateButtonsVisibility(content.text)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (enabled) {
            updateButtonsVisibility(content.text)
        } else {
            binder.actionInputFieldAction.makeGone()
            binder.actionInputFieldClear.makeGone()
        }

        content.isEnabled = enabled
    }

    private fun updateButtonsVisibility(text: CharSequence?) {
        val clipboardValue = clipboardManager?.getTextOrNull()
        val clipboardIsNotEmpty = !TextUtils.isEmpty(clipboardValue)
        val textIsEmpty = TextUtils.isEmpty(text)

        binder.actionInputFieldClear.isGone = textIsEmpty
        binder.actionInputFieldAction.isVisible = textIsEmpty && clipboardIsNotEmpty
    }

    private fun setBackgrounds() = with(context) {
        background = context.getInputBackground()

        binder.actionInputFieldAction.background = buttonBackground()
    }

    private fun paste() {
        val clipboard = clipboardManager?.getTextOrNull()
        content.setText(clipboard)
    }

    private fun Context.buttonBackground() = addRipple(getRoundedCornerDrawable(R.color.button_background_secondary, cornerSizeInDp = 10))

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.ActionInputField) {
        val hint = it.getString(R.styleable.ActionInputField_android_hint)
        hint?.let { content.hint = hint }
    }

    private fun getClipboardManager(): ClipboardManager? {
        return if (isInEditMode) {
            null
        } else {
            FeatureUtils.getCommonApi(context)
                .provideClipboardManager()
        }
    }
}
