package io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites

import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
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
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentAddToFavouritesBinding
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon

import javax.inject.Inject

private const val PAYLOAD_KEY = "DAppSignExtrinsicFragment.Payload"

class AddToFavouritesFragment : BaseFragment<AddToFavouritesViewModel, FragmentAddToFavouritesBinding>() {

    companion object {

        fun getBundle(payload: AddToFavouritesPayload) = bundleOf(PAYLOAD_KEY to payload)
    }

    override fun createBinding() = FragmentAddToFavouritesBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.addToFavouritesToolbar.applyStatusBarInsets()
        binder.addToFavouritesToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        binder.addToFavouritesToolbar.setRightActionClickListener { viewModel.saveClicked() }
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
        binder.addToFavouritesTitleInput.bindTo(viewModel.labelFlow, lifecycleScope)
        binder.addToFavouritesAddressInput.bindTo(viewModel.urlFlow, lifecycleScope)

        viewModel.iconLink.observe {
            binder.addToFavouritesIcon.showDAppIcon(it, imageLoader)
        }

        viewModel.focusOnAddressFieldEvent.observeEvent {
            binder.addToFavouritesTitleInput.postToSelf {
                showSoftKeyboard()

                moveCursorToTheEnd()
            }
        }
    }
}
