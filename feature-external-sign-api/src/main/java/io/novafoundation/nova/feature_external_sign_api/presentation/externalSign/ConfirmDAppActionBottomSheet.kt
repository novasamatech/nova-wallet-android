package io.novafoundation.nova.feature_external_sign_api.presentation.externalSign

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_external_sign_api.R
import io.novafoundation.nova.feature_external_sign_api.databinding.BottomSheetConfirmDappActionBinding

abstract class ConfirmDAppActionBottomSheet(
    context: Context,
    private val onConfirm: () -> Unit,
    private val onDeny: () -> Unit
) : BaseBottomSheet<BottomSheetConfirmDappActionBinding>(context), DialogExtensions {

    override val binder: BottomSheetConfirmDappActionBinding = BottomSheetConfirmDappActionBinding.inflate(LayoutInflater.from(context))

    abstract val contentBinder: ViewBinding

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_sheet_confirm_dapp_action)
        super.onCreate(savedInstanceState)

        setCancelable(false)

        binder.confirmInnerContent.addView(contentBinder.root)

        binder.confirmDAppActionAllow.setDismissingClickListener { onConfirm() }
        binder.confirmDAppActionReject.setDismissingClickListener { onDeny() }
    }
}
