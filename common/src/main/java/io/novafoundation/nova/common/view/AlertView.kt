package io.novafoundation.nova.common.view

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewAlertBinding
import io.novafoundation.nova.common.databinding.ViewAlertMessageBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes

typealias SimpleAlertModel = String

class AlertModel(
    val style: AlertView.Style,
    val message: String,
    val subMessages: List<CharSequence>,
    val action: ActionModel? = null
) {

    constructor(
        style: AlertView.Style,
        message: String,
        subMessage: CharSequence? = null,
        action: ActionModel? = null
    ) : this(style, message, subMessages = listOfNotNull(subMessage), action)

    class ActionModel(val text: String, val listener: () -> Unit)
}

class AlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    enum class StylePreset {
        WARNING, ERROR, INFO
    }

    data class Style(
        @DrawableRes val iconRes: Int,
        @ColorRes val backgroundColorRes: Int,
        @ColorRes val iconTintRes: Int? = null,
        val iconGravity: Int = Gravity.TOP
    ) {

        companion object {

            fun fromPreset(preset: StylePreset, iconGravity: Int = Gravity.TOP) = when (preset) {
                StylePreset.WARNING -> Style(R.drawable.ic_warning_filled, R.color.warning_block_background, iconGravity = iconGravity)
                StylePreset.ERROR -> Style(R.drawable.ic_slash, R.color.error_block_background, iconGravity = iconGravity)
                StylePreset.INFO -> Style(R.drawable.ic_info_accent, R.color.individual_chip_background, iconGravity = iconGravity)
            }
        }
    }

    private val binder = ViewAlertBinding.inflate(inflater(), this)

    init {
        updatePadding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)

        attrs?.let(::applyAttrs)
    }

    fun setStyle(style: Style) {
        setStyleBackground(style.backgroundColorRes)
        setStyleIcon(style.iconRes, style.iconTintRes, style.iconGravity)
    }

    fun setStylePreset(preset: StylePreset) {
        setStyle(Style.fromPreset(preset))
    }

    fun setMessage(text: String) {
        binder.alertMessage.text = text
    }

    fun setMessage(@StringRes textRes: Int) {
        binder.alertMessage.setText(textRes)
    }

    fun setSubMessage(text: CharSequence?) {
        setSubMessages(listOfNotNull(text))
    }

    fun setSubMessages(subMessages: List<CharSequence>) {
        binder.alertSubMessageContainer.removeAllViews()
        subMessages.forEach { createSubMessageView(it) }
    }

    fun setActionText(actionText: String?) {
        binder.alertActionGroup.letOrHide(actionText) { text ->
            binder.alertActionContent.text = text
        }
    }

    fun setOnActionClickedListener(listener: () -> Unit) {
        binder.alertActionContent.setOnClickListener { listener() }
        binder.alertActionArrow.setOnClickListener { listener() }
    }

    fun setModel(maybeModel: SimpleAlertModel?) = letOrHide(maybeModel) { model ->
        setMessage(model)
    }

    private fun setStyleBackground(@ColorRes colorRes: Int) {
        background = getRoundedCornerDrawable(fillColorRes = colorRes)
    }

    private fun setStyleIcon(@DrawableRes iconRes: Int, iconTintRes: Int? = null, iconGravity: Int) {
        binder.alertIcon.setImageResource(iconRes)
        binder.alertIcon.setImageTintRes(iconTintRes)
        binder.alertIcon.updateLayoutParams<FrameLayout.LayoutParams> { gravity = iconGravity }
    }

    private fun createSubMessageView(text: CharSequence): TextView {
        return ViewAlertMessageBinding.inflate(inflater(), binder.alertSubMessageContainer, true)
            .alertSubMessage
            .apply {
                this.text = text
                this.movementMethod = LinkMovementMethod.getInstance()
            }
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.AlertView) {
        val stylePreset = it.getEnum(R.styleable.AlertView_alertMode, StylePreset.WARNING)
        val styleFromPreset = Style.fromPreset(stylePreset)

        val backgroundColorRes = it.getResourceId(R.styleable.AlertView_styleBackgroundColor, styleFromPreset.backgroundColorRes)
        val iconRes = it.getResourceId(R.styleable.AlertView_styleIcon, styleFromPreset.iconRes)
        val iconTintRes = it.getResourceIdOrNull(R.styleable.AlertView_styleIconTint)

        setStyle(Style(iconRes, backgroundColorRes, iconTintRes))

        val text = it.getString(R.styleable.AlertView_android_text)
        text?.let(::setMessage)

        val description = it.getString(R.styleable.AlertView_AlertView_description)
        setSubMessage(description)

        val action = it.getString(R.styleable.AlertView_AlertView_action)
        setActionText(action)
    }
}

fun AlertView.setModel(model: AlertModel) {
    setMessage(model.message)
    setSubMessages(model.subMessages)

    if (model.action != null) {
        setActionText(model.action.text)
        setOnActionClickedListener(model.action.listener)
    }

    setStyle(model.style)
}

fun AlertView.setModelOrHide(maybeModel: AlertModel?) = letOrHide(maybeModel, ::setModel)

fun AlertView.setMessageOrHide(text: String?) = letOrHide(text, ::setMessage)

fun AlertView.StylePreset.asStyle(): AlertView.Style {
    return AlertView.Style.fromPreset(this)
}
