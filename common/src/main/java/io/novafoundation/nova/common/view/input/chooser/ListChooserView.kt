package io.novafoundation.nova.common.view.input.chooser

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.ensureSuffix
import io.novafoundation.nova.common.utils.useAttributes

class ListChooserView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_list_chooser, this)

        orientation = HORIZONTAL

        attrs?.let(::applyAttributes)
    }

    fun setLabel(label: String) {
        val suffixedLabel = label.ensureSuffix(":")

        viewListChooserLabel.text = suffixedLabel
    }

    fun setValueDisplay(value: String?) {
        viewListChooserValue.text = value
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.ListChooserView) {
        val label = it.getString(R.styleable.ListChooserView_listChooserView_label)
        label?.let(::setLabel)

        val initialValueDisplay = it.getString(R.styleable.ListChooserView_listChooserView_value)
        initialValueDisplay?.let(::setValueDisplay)
    }
}

fun ListChooserView.setModel(model: ListChooserMixin.Model<*>) {
    setValueDisplay(model.display)
}
