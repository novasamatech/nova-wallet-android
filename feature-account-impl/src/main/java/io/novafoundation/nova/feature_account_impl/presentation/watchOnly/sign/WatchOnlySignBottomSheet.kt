package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.sign

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.ActionNotAllowedBottomSheet
import io.novafoundation.nova.feature_account_impl.R

class WatchOnlySignBottomSheet(
    context: Context,
    onSuccess: () -> Unit
) : ActionNotAllowedBottomSheet(context, onSuccess) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title.setText(R.string.account_watch_key_missing_title)
        subtitle.setText(R.string.account_watch_key_missing_description)

        applyDashedIconStyle(R.drawable.ic_key_missing)
    }
}
