package io.novafoundation.nova.feature_external_sign_api.presentation.externalSign

import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_external_sign_api.R

abstract class ConfirmDAppActionBottomSheet(
    context: Context,
    private val onConfirm: () -> Unit,
    private val onDeny: () -> Unit
) : BaseBottomSheet(context), DialogExtensions {

    @get:LayoutRes
    abstract val contentLayoutRes: Int

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_sheet_confirm_dapp_action)
        super.onCreate(savedInstanceState)

        setCancelable(false)

        confirmInnerContent.inflateChild(contentLayoutRes, attachToRoot = true)

        confirmDAppActionAllow.setDismissingClickListener { onConfirm() }
        confirmDAppActionReject.setDismissingClickListener { onDeny() }
    }
}
