package io.novafoundation.nova.feature_account_impl.presentation.proxy.sign

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.BottomSheetProxyWarningBinding

class ProxySignWarningBottomSheet(
    context: Context,
    private val subtitle: CharSequence,
    private val onFinish: (Boolean) -> Unit,
    private val dontShowAgain: () -> Unit
) : BaseBottomSheet<BottomSheetProxyWarningBinding>(context, io.novafoundation.nova.common.R.style.BottomSheetDialog),
    WithContextExtensions by WithContextExtensions(context) {

    override val binder: BottomSheetProxyWarningBinding = BottomSheetProxyWarningBinding.inflate(LayoutInflater.from(context))

    private var finishWithContinue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder.proxySigningWarningMessage.setText(subtitle)

        binder.proxySigningWarningContinue.setDismissingClickListener {
            if (binder.proxySigningWarningDontShowAgain.isChecked) {
                dontShowAgain()
            }
            finishWithContinue = true
        }

        binder.proxySigningWarningCancel.setDismissingClickListener {
            finishWithContinue = false
        }

        setOnDismissListener { onFinish(finishWithContinue) }
    }
}
