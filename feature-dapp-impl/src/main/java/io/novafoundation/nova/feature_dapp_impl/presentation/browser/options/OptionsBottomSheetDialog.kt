package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options

import android.content.Context
import android.view.View
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.switcherItem
import io.novafoundation.nova.feature_dapp_impl.R

class OptionsBottomSheetDialog(
    context: Context,
    private val callback: Callback,
    private val payload: DAppOptionsPayload
) : FixedListBottomSheet(context) {

    init {
        setTitle(R.string.dapp_options_title)

        switcherItem(
            R.drawable.ic_desktop,
            R.string.dapp_options_desktop_mode,
            payload.isDesktopModeEnabled,
            ::toggleDesktopMode
        )
    }

    private fun toggleDesktopMode(view: View) {
        callback.onDesktopModeClick()
    }

    interface Callback {
        fun onDesktopModeClick()
    }
}
