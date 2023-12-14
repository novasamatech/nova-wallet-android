package io.novafoundation.nova.feature_account_impl.presentation.proxy.sign

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.ActionNotAllowedBottomSheet
import io.novafoundation.nova.feature_account_impl.R

class ProxySignNotEnoughPermissionBottomSheet(
    context: Context,
    private val subtitle: CharSequence,
    onSuccess: () -> Unit
) : ActionNotAllowedBottomSheet(context, onSuccess) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleView.setText(R.string.proxy_signing_not_enough_permission_title)
        subtitleView.setText(subtitle)
        buttonView.setText(R.string.common_okay_back)

        applySolidIconStyle(R.drawable.ic_proxy)
    }
}
