package io.novafoundation.nova.feature_account_impl.presentation.proxy.sign

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.synthetic.main.bottom_sheet_proxy_warning.proxySigningWarningCancel
import kotlinx.android.synthetic.main.bottom_sheet_proxy_warning.proxySigningWarningContinue
import kotlinx.android.synthetic.main.bottom_sheet_proxy_warning.proxySigningWarningDontShowAgain
import kotlinx.android.synthetic.main.bottom_sheet_proxy_warning.proxySigningWarningMessage

class ProxySignWarningBottomSheet(
    context: Context,
    private val subtitle: CharSequence,
    private val onFinish: (Boolean) -> Unit,
    private val dontShowAgain: () -> Unit
) : BaseBottomSheet(context, io.novafoundation.nova.common.R.style.BottomSheetDialog), WithContextExtensions by WithContextExtensions(context) {

    private var finishWithContinue = false

    init {
        setContentView(R.layout.bottom_sheet_proxy_warning)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        proxySigningWarningMessage.setText(subtitle)

        proxySigningWarningContinue.setOnClickListener {
            if (proxySigningWarningDontShowAgain.isChecked) {
                dontShowAgain()
            }
            finishWithContinue = true
            dismiss()
        }

        proxySigningWarningCancel.setOnClickListener {
            finishWithContinue = false
            dismiss()
        }

        setOnDismissListener { onFinish(finishWithContinue) }
    }
}
