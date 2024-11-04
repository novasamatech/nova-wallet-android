package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Connect.Instruction
import io.novafoundation.nova.feature_account_impl.databinding.ViewInstructionImageBinding

class InstructionImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    companion object {

        fun createWithDefaultLayoutParams(context: Context): InstructionImageView {
            return InstructionImageView(context).apply {
                layoutParams = MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(48.dp(context), 16.dp(context), 16.dp(context), 0)
                }
            }
        }
    }

    private val binder = ViewInstructionImageBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL
    }

    fun setModel(model: Instruction.Image) {
        binder.viewInstructionImage.setImageResource(model.imageRes)
        binder.viewInstructionImageLabel.text = model.label
    }
}
