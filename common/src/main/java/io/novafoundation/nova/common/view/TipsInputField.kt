package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewTipsInputBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.common.view.shape.getInputBackgroundError

class TipsInputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context), ValidatableInputField {

    private val binder = ViewTipsInputBinding.inflate(inflater(), this)

    private var postfix: String? = null
    private var postfixPadding = 4.dp
    private var postfixPosition: PointF? = null

    private val textPaint: Paint

    val content: EditText
        get() = binder.tipsInputField

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.view_tips_input, this)
        binder.tipsInputFieldContainer.setAddStatesFromChildren(true)
        binder.tipsInputFieldContainer.background = context.getInputBackground()

        content.onTextChanged {
            binder.tipsInputClear.isVisible = it.isNotEmpty()
            binder.tipsInputContainer.isVisible = it.isEmpty()
            measurePostfix(it)
            invalidate()
        }

        textPaint = Paint(content.paint).apply {
            color = content.currentTextColor
        }

        binder.tipsInputClear.setOnClickListener { content.text = null }

        attrs?.let(::applyAttributes)
        setWillNotDraw(false)
    }

    private fun measurePostfix(text: String) {
        if (text.isEmpty() || postfix == null) {
            postfixPosition = null
            return
        }

        val textWidth = content.paint.measureText(text, 0, text.length)
        val textLeft = content.paddingLeft.toFloat()
        postfixPosition = PointF(
            content.x + textLeft + textWidth,
            content.y + content.baseline.toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (postfix != null && postfixPosition != null) {
            canvas.drawText(postfix!!, postfixPosition!!.x + postfixPadding, postfixPosition!!.y, textPaint)
        }
    }

    fun setHint(hint: String) {
        content.hint = hint
    }

    fun clearTips() {
        binder.tipsInputContainer.removeAllViews()
    }

    fun addIconTip(@DrawableRes iconRes: Int, @ColorRes tintRes: Int? = null, onClick: OnClickListener): View {
        val view = ImageView(context)
        prepareCommonOptions(view, onClick)
        view.setImageResource(iconRes)
        view.setPadding(8.dp, 0.dp, 8.dp, 0.dp)
        view.setImageTintRes(tintRes)
        view.scaleType = ImageView.ScaleType.CENTER_INSIDE
        binder.tipsInputContainer.addView(view)
        return view
    }

    fun addTextTip(text: String, @ColorRes tintRes: Int? = null, onClick: OnClickListener): View {
        val view = TextView(context)
        prepareCommonOptions(view, onClick)
        view.text = text
        view.setTextAppearance(R.style.TextAppearance_NovaFoundation_SemiBold_Footnote)
        view.setPadding(12.dp, 0.dp, 12.dp, 0.dp)
        tintRes?.let { view.setTextColor(context.getColor(it)) }
        view.gravity = Gravity.CENTER
        binder.tipsInputContainer.addView(view)
        return view
    }

    private fun prepareCommonOptions(view: View, onClick: OnClickListener) {
        view.background = buttonBackground()
        view.layoutParams = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
            marginStart = 4.dp
            marginEnd = 4.dp
        }
        view.setOnClickListener(onClick)
    }

    override fun showError(error: String) {
        binder.tipsInputError.makeVisible()
        binder.tipsInputError.text = error
        val color = context.getColor(R.color.text_negative)
        content.setTextColor(color)
        textPaint.color = color
        binder.tipsInputFieldContainer.background = context.getInputBackgroundError()
        invalidate()
    }

    override fun hideError() {
        binder.tipsInputError.makeGone()
        val color = context.getColor(R.color.text_primary)
        content.setTextColor(color)
        textPaint.color = color
        binder.tipsInputFieldContainer.background = context.getInputBackground()
        invalidate()
    }

    private fun buttonBackground() = addRipple(getRoundedCornerDrawable(R.color.button_background_secondary))

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.TipsInputField) {
        val hint = it.getString(R.styleable.TipsInputField_android_hint)
        hint?.let { content.hint = hint }

        postfix = it.getString(R.styleable.TipsInputField_postfix)

        val digits = it.getString(R.styleable.TipsInputField_android_digits)
        digits?.let {
            content.keyListener = DigitsKeyListener.getInstance(it)
        }

        val inputType = it.getInt(R.styleable.TipsInputField_android_inputType, EditorInfo.TYPE_NULL)
        content.inputType = inputType
    }
}
