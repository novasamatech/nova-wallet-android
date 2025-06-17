package io.novafoundation.nova.feature_account_impl.presentation.sign

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_account_impl.databinding.BottomSheetNestedWarningBinding

class NestedSignWarningBottomSheet(
    context: Context,
    private val subtitle: CharSequence,
    private val title: String,
    @DrawableRes private val iconRes: Int,
    private val onFinish: (Boolean) -> Unit,
    private val dontShowAgain: () -> Unit
) : BaseBottomSheet<BottomSheetNestedWarningBinding>(context, io.novafoundation.nova.common.R.style.BottomSheetDialog),
    WithContextExtensions by WithContextExtensions(context) {

    override val binder: BottomSheetNestedWarningBinding = BottomSheetNestedWarningBinding.inflate(LayoutInflater.from(context))

    private var finishWithContinue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder.nestedSigningWarningTitle.text = title
        binder.nestedSigningWarningMessage.text = subtitle
        binder.nestedSigningWarningIcon.setImageResource(iconRes)

        binder.nestedSigningWarningContinue.setDismissingClickListener {
            if (binder.nestedSigningWarningDontShowAgain.isChecked) {
                dontShowAgain()
            }
            finishWithContinue = true
        }

        binder.nestedSigningWarningCancel.setDismissingClickListener {
            finishWithContinue = false
        }

        setOnDismissListener { onFinish(finishWithContinue) }
    }
}
