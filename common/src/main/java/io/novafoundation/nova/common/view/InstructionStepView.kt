package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.getResourceIdOrThrow
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_instruction_step.view.instructionStepIndicator
import kotlinx.android.synthetic.main.view_instruction_step.view.instructionStepText

class InstructionStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        orientation = HORIZONTAL

        View.inflate(context, R.layout.view_instruction_step, this)

        attrs?.let(::applyAttributes)
    }

    fun setStepNumber(stepNumber: Int) {
        instructionStepIndicator.text = stepNumber.toString()
    }

    fun setStepText(stepText: CharSequence) {
        instructionStepText.text = stepText
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.InstructionStepView) {
        val stepNumber = it.getString(R.styleable.InstructionStepView_stepNumber)
        instructionStepIndicator.text = stepNumber

        // use getResourceId() instead of getString() since resources might contain spans which will be lost if getString() is used
        val stepText = it.getResourceIdOrThrow(R.styleable.InstructionStepView_stepText)
        instructionStepText.setText(stepText)
    }
}
