package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewInstructionStepBinding
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes

class InstructionStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewInstructionStepBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL

        attrs?.let(::applyAttributes)
    }

    fun setStepNumber(stepNumber: Int) {
        binder.instructionStepIndicator.text = stepNumber.toString()
    }

    fun setStepText(stepText: CharSequence) {
        binder.instructionStepText.text = stepText
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.InstructionStepView) {
        val stepNumber = it.getString(R.styleable.InstructionStepView_stepNumber)
        binder.instructionStepIndicator.text = stepNumber

        // use getResourceId() instead of getString() since resources might contain spans which will be lost if getString() is used
        val stepText = it.getResourceIdOrNull(R.styleable.InstructionStepView_stepText)
        stepText?.let { binder.instructionStepText.setText(it) }
    }
}
