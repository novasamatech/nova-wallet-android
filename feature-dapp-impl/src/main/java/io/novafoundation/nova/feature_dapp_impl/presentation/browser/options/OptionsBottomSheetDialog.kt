package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options

import android.content.Context
import android.view.View
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.switcherItem
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_dapp_impl.R

class OptionsBottomSheetDialog(
    context: Context,
    private val callback: Callback,
    private val payload: DAppOptionsPayload
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context)) {

    init {
        setTitle(R.string.dapp_options_title)

        textItem(
            if (payload.isFavorite) R.drawable.ic_unfavorite_heart_outline else R.drawable.ic_favorite_heart_outline,
            if (payload.isFavorite) R.string.dapp_options_remove_from_favorite else R.string.dapp_options_add_to_favorite,
            showArrow = false,
            applyIconTint = true,
            ::toggleFavorite
        )

        switcherItem(
            R.drawable.ic_desktop,
            R.string.dapp_options_desktop_mode,
            payload.isDesktopModeEnabled,
            ::toggleDesktopMode
        )
    }

    private fun toggleFavorite(view: View) {
        callback.onFavoriteClick(payload)
    }

    private fun toggleDesktopMode(view: View) {
        callback.onDesktopModeClick()
    }

    interface Callback {
        fun onFavoriteClick(payload: DAppOptionsPayload)
        fun onDesktopModeClick()
    }
}
