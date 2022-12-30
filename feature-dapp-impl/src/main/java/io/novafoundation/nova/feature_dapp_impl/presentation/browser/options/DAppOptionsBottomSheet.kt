package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.setupRemoveFavouritesConfirmation
import kotlinx.android.synthetic.main.fragment_dapps_options.dappOptionDesktopMode
import kotlinx.android.synthetic.main.fragment_dapps_options.dappOptionFavorite

class DAppOptionsBottomSheet() : BaseBottomSheetFragment<DAppOptionsViewModel>() {

    companion object {
        private const val KEY_PAYLOAD = "KEY_PAYLOAD"

        fun getBundle(payload: DAppOptionsPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_dapps_options, container, false)

    override fun initViews() {
        val payload = viewModel.payload

        val favoriteText = if (payload.isFavorite) R.string.dapp_options_remove_from_favorite else R.string.dapp_options_add_to_favorite
        val favoriteIcon = if (payload.isFavorite) R.drawable.ic_unfavorite_heart_outline else R.drawable.ic_favorite_heart_outline

        dappOptionFavorite.setText(favoriteText)
        dappOptionFavorite.setDrawableStart(favoriteIcon, paddingInDp = 12, tint = R.color.icon_primary)

        dappOptionDesktopMode.isChecked = payload.isDesktopModeEnabled
        dappOptionDesktopMode.isGone = payload.isDesktopModeOnly

        dappOptionFavorite.setOnClickListener { viewModel.favoriteClick() }
        dappOptionDesktopMode.setOnClickListener { viewModel.desktopModeClick() }
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .dappOptionsComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: DAppOptionsViewModel) {
        setupRemoveFavouritesConfirmation(viewModel.removeFromFavouritesConfirmation)
    }
}
