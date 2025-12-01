package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.databinding.ViewInstructionImageBinding
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide

class InstructionImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewInstructionImageBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL
    }

    fun setModel(@DrawableRes imageRes: Int, label: String?) {
        binder.viewInstructionImage.setImageResource(imageRes)
        binder.viewInstructionImageLabel.setTextOrHide(label)
    }
}
