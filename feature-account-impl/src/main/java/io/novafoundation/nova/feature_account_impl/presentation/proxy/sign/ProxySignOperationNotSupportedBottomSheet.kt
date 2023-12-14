package io.novafoundation.nova.feature_account_impl.presentation.proxy.sign

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.ActionNotAllowedBottomSheet
import io.novafoundation.nova.feature_account_impl.R

class ProxySignOperationNotSupportedBottomSheet(
    context: Context,
    onSuccess: () -> Unit
) : ActionNotAllowedBottomSheet(context, onSuccess) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleView.setText(R.string.proxy_signing_is_not_supported_title)
        subtitleView.setText(R.string.proxy_signing_is_not_supported_message)
        buttonView.setText(R.string.common_okay_back)

        applySolidIconStyle(R.drawable.ic_proxy)
    }
}
