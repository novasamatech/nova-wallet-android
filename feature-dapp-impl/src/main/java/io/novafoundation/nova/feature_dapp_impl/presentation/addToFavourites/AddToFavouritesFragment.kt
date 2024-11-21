package io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.moveCursorToTheEnd
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import kotlinx.android.synthetic.main.fragment_add_to_favourites.addToFavouritesAddressInput
import kotlinx.android.synthetic.main.fragment_add_to_favourites.addToFavouritesIcon
import kotlinx.android.synthetic.main.fragment_add_to_favourites.addToFavouritesTitleInput
import kotlinx.android.synthetic.main.fragment_add_to_favourites.addToFavouritesToolbar
import javax.inject.Inject

private const val PAYLOAD_KEY = "DAppSignExtrinsicFragment.Payload"

class AddToFavouritesFragment : BaseFragment<AddToFavouritesViewModel>() {

    companion object {

        fun getBundle(payload: AddToFavouritesPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_add_to_favourites, container, false)
    }

    override fun initViews() {
        addToFavouritesToolbar.applyStatusBarInsets()
        addToFavouritesToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        addToFavouritesToolbar.setRightActionClickListener { viewModel.saveClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        hideKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .addToFavouritesComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subscribe(viewModel: AddToFavouritesViewModel) {
        addToFavouritesTitleInput.bindTo(viewModel.labelFlow, lifecycleScope)
        addToFavouritesAddressInput.bindTo(viewModel.urlFlow, lifecycleScope)

        viewModel.iconLink.observe {
            addToFavouritesIcon.showDAppIcon(it, imageLoader)
        }

        viewModel.focusOnAddressFieldEvent.observeEvent {
            addToFavouritesTitleInput.postToSelf {
                showSoftKeyboard()

                moveCursorToTheEnd()
            }
        }
    }
}
