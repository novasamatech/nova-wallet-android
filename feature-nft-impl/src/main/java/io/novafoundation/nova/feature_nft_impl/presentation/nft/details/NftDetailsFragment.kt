package io.novafoundation.nova.feature_nft_impl.presentation.nft.details

import android.text.TextUtils
import android.view.View
import androidx.core.os.bundleOf

import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.databinding.FragmentNftDetailsBinding
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.model.NftPriceModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.PriceSectionView

import javax.inject.Inject

class NftDetailsFragment : BaseFragment<NftDetailsViewModel, FragmentNftDetailsBinding>() {

    companion object {

        private const val PAYLOAD = "NftDetailsFragment.PAYLOAD"

        fun getBundle(nftId: String) = bundleOf(PAYLOAD to nftId)
    }

    override fun createBinding() = FragmentNftDetailsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val contentViews by lazy(LazyThreadSafetyMode.NONE) {
        listOf(
            binder.nftDetailsMedia,
            binder.nftDetailsTitle,
            binder.nftDetailsDescription,
            binder.nftDetailsIssuance,
            binder.nftDetailsPrice,
            binder.nftDetailsTable
        )
    }

    override fun initViews() {
        binder.nftDetailsToolbar.applyStatusBarInsets()
        binder.nftDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.nftDetailsOnwer.setOnClickListener { viewModel.ownerClicked() }
        binder.nftDetailsCreator.setOnClickListener { viewModel.creatorClicked() }

        binder.nftDetailsCollection.valuePrimary.ellipsize = TextUtils.TruncateAt.END

        binder.nftDetailsProgress.makeVisible()
        contentViews.forEach(View::makeGone)
    }

    override fun inject() {
        FeatureUtils.getFeature<NftFeatureComponent>(this, NftFeatureApi::class.java)
            .nftDetailsComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: NftDetailsViewModel) {
        setupExternalActions(viewModel)

        viewModel.nftDetailsUi.observe {
            binder.nftDetailsProgress.makeGone()
            contentViews.forEach(View::makeVisible)

            binder.nftDetailsMedia.load(it.media, imageLoader) {
                placeholder(R.drawable.nft_media_progress)
                error(R.drawable.nft_media_progress)
            }
            binder.nftDetailsTitle.text = it.name
            binder.nftDetailsDescription.setTextOrHide(it.description)
            binder.nftDetailsIssuance.text = it.issuance

            binder.nftDetailsPrice.setPriceOrHide(it.price)

            if (it.collection != null) {
                binder.nftDetailsCollection.makeVisible()
                binder.nftDetailsCollection.loadImage(it.collection.media)
                binder.nftDetailsCollection.showValue(it.collection.name)
            } else {
                binder.nftDetailsCollection.makeGone()
            }

            binder.nftDetailsOnwer.showAddress(it.owner)

            if (it.creator != null) {
                binder.nftDetailsCreator.makeVisible()
                binder.nftDetailsCreator.showAddress(it.creator)
            } else {
                binder.nftDetailsCreator.makeGone()
            }

            binder.nftDetailsChain.showChain(it.network)
        }

        viewModel.exitingErrorLiveData.observeEvent {
            errorDialog(requireContext(), onConfirm = viewModel::backClicked) {
                setMessage(it)
            }
        }
    }

    private fun PriceSectionView.setPriceOrHide(maybePrice: NftPriceModel?) = letOrHide(maybePrice) { price ->
        setPrice(price.amountInfo, price.fiat)
    }
}
