package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getCornersStateDrawable
import kotlinx.android.synthetic.main.view_labeled_text.view.labeledTextAction
import kotlinx.android.synthetic.main.view_labeled_text.view.labeledTextIcon
import kotlinx.android.synthetic.main.view_labeled_text.view.labeledTextLabel
import kotlinx.android.synthetic.main.view_labeled_text.view.labeledTextPrimaryIcon
import kotlinx.android.synthetic.main.view_labeled_text.view.labeledTextText

class LabeledTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_labeled_text, this)

        minHeight = 48.dp
        setPadding(0, 8.dp, 0, 8.dp)

        with(context) {
            background = addRipple(getCornersStateDrawable())
        }

        applyAttributes(attrs)
    }

    private var singleLine: Boolean = true

    val textIconView: ImageView
        get() = labeledTextIcon

    val primaryIcon: ImageView
        get() = labeledTextPrimaryIcon

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LabeledTextView)

            val label = typedArray.getString(R.styleable.LabeledTextView_label)
            setLabelOrHide(label)

            val message = typedArray.getString(R.styleable.LabeledTextView_message)
            message?.let(::setMessage)

            val messageColor = typedArray.getColor(R.styleable.LabeledTextView_messageColor, context.getColor(R.color.text_primary))
            setMessageColor(messageColor)

            val messageStyle = typedArray.getResourceIdOrNull(R.styleable.LabeledTextView_messageStyle)
            messageStyle?.let(labeledTextText::setTextAppearance)

            val labelStyle = typedArray.getResourceIdOrNull(R.styleable.LabeledTextView_labelStyle)
            labelStyle?.let(labeledTextLabel::setTextAppearance)

            val textIcon = typedArray.getDrawable(R.styleable.LabeledTextView_textIcon)
            textIcon?.let(::setTextIcon)

            val enabled = typedArray.getBoolean(R.styleable.LabeledTextView_enabled, true)
            isEnabled = enabled

            val actionIcon = typedArray.getDrawable(R.styleable.LabeledTextView_actionIcon)
            setActionIcon(actionIcon)

            singleLine = typedArray.getBoolean(R.styleable.LabeledTextView_android_singleLine, true)
            labeledTextText.isSingleLine = singleLine

            typedArray.recycle()
        }
    }

    private fun setMessageColor(messageColor: Int) {
        labeledTextText.setTextColor(messageColor)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        labeledTextAction.setVisible(enabled)
    }

    fun setLabel(label: String) {
        setLabelOrHide(label)
    }

    fun setLabelOrHide(label: String?) {
        labeledTextLabel.setTextOrHide(label)
    }

    fun setActionIcon(icon: Drawable?) {
        labeledTextAction.setImageDrawable(icon)

        labeledTextAction.setVisible(icon != null)
    }

    fun setMessage(@StringRes messageRes: Int) = setMessage(context.getString(messageRes))

    fun setMessage(text: String?) {
        labeledTextText.text = text
    }

    fun setTextIcon(@DrawableRes iconRes: Int) = setTextIcon(context.getDrawableCompat(iconRes))

    fun setTextIcon(icon: Drawable) {
        labeledTextIcon.makeVisible()
        labeledTextIcon.setImageDrawable(icon)
    }

    fun setPrimaryIcon(icon: Drawable) {
        primaryIcon.makeVisible()
        primaryIcon.setImageDrawable(icon)
    }

    fun setActionClickListener(listener: (View) -> Unit) {
        labeledTextAction.setOnClickListener(listener)
    }

    fun setWholeClickListener(listener: (View) -> Unit) {
        setOnClickListener(listener)

        setActionClickListener(listener)
    }
}
