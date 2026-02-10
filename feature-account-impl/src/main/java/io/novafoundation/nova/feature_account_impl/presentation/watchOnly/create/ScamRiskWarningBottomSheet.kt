package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_account_impl.databinding.BottomSheetScamRiskWarningBinding

class ScamRiskWarningBottomSheet(
    context: Context,
    val payload: Payload,
    val onConfirmClicked: () -> Unit,
    val onMailClicked: () -> Unit
) : BaseBottomSheet<BottomSheetScamRiskWarningBinding>(context, R.style.BottomSheetDialog) {

    class Payload(val supportMail: String)

    override val binder: BottomSheetScamRiskWarningBinding = BottomSheetScamRiskWarningBinding.inflate(LayoutInflater.from(context))

    init {
        binder.scamWarningBottomSheetSubtitle.movementMethod = LinkMovementMethod.getInstance()
        binder.scamWarningBottomSheetSubtitle.text = getSubtitle()

        binder.scamWarningBottomSheetConfirm.setOnClickListener {
            onConfirmClicked()
            dismiss()
        }

        binder.scamWarningBottomSheetCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun getSubtitle(): CharSequence {
        val subtitle = context.getString(R.string.watch_only_scam_risk_warning_subtitle)
        val highlightedPart = context.getString(R.string.watch_only_scam_risk_warning_subtitle_highlighted)
            .addColor(context.getColor(R.color.text_primary))
        val mailPart = payload.supportMail
            .addColor(context.getColor(R.color.button_text_accent))
            .setFullSpan(clickableSpan { onMailClicked() })

        return SpannableFormatter.format(subtitle, highlightedPart, mailPart)
    }
}
