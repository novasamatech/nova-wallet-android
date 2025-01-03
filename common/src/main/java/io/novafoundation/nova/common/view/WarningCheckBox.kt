package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewWarningCheckboxBinding
import io.novafoundation.nova.common.utils.CheckableListener
import io.novafoundation.nova.common.utils.getColorOrNull
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageResourceOrHide
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.useAttributes

class WarningCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), CheckableListener {

    private val binder = ViewWarningCheckboxBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.secondary_container_ripple_background)
        addStatesFromChildren()

        attrs?.let(::applyAttributes)
    }

    fun setText(text: CharSequence?) {
        binder.warningCheckBoxCheckBox.text = text
    }

    fun setIconTintColor(color: Int) {
        binder.warningCheckBoxIcon.setImageTint(color)
    }

    fun setIcon(@DrawableRes iconRes: Int?) {
        binder.warningCheckBoxIcon.setImageResourceOrHide(iconRes)
    }

    override fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener) {
        binder.warningCheckBoxCheckBox.setOnCheckedChangeListener(listener)
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.WarningCheckBox) {
        val iconRes = it.getResourceIdOrNull(R.styleable.WarningCheckBox_android_icon)
        val iconTint = it.getColorOrNull(R.styleable.WarningCheckBox_iconTint)
        val text = it.getString(R.styleable.WarningCheckBox_android_text)

        setIcon(iconRes)
        iconTint?.let(::setIconTintColor)
        setText(text)
    }

    override fun setChecked(checked: Boolean) {
        binder.warningCheckBoxCheckBox.isChecked = checked
    }

    override fun isChecked(): Boolean {
        return binder.warningCheckBoxCheckBox.isChecked
    }

    override fun toggle() {
        binder.warningCheckBoxCheckBox.toggle()
    }
}
