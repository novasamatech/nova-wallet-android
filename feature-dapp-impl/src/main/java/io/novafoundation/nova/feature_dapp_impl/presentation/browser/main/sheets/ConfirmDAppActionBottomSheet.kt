package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.sheets

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation
import kotlinx.android.synthetic.main.bottom_sheet_confirm_dapp_action.confirmDAppActionAllow
import kotlinx.android.synthetic.main.bottom_sheet_confirm_dapp_action.confirmDAppActionReject
import kotlinx.android.synthetic.main.bottom_sheet_confirm_dapp_action.confirmInnerContent

abstract class ConfirmDAppActionBottomSheet<A : DappPendingConfirmation.Action>(
    context: Context,
    protected val confirmation: DappPendingConfirmation<A>,
) : BottomSheetDialog(context, R.style.BottomSheetDialog), DialogExtensions {

    final override val dialogInterface: DialogInterface
        get() = this

    @get:LayoutRes
    abstract val contentLayoutRes: Int

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_sheet_confirm_dapp_action)
        super.onCreate(savedInstanceState)

        confirmInnerContent.inflateChild(contentLayoutRes, attachToRoot = true)

        confirmDAppActionAllow.setDismissingClickListener { confirmation.onConfirm() }
        confirmDAppActionReject.setDismissingClickListener { confirmation.onDeny() }

        setOnCancelListener { confirmation.onCancel() }
    }
}
