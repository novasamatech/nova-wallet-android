package io.novafoundation.nova.feature_nft_impl.presentation.nft.details

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.setPriceOrHide
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsChain
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsCollection
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsCreator
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsDescription
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsIssuance
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsMedia
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsOnwer
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsPrice
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsProgress
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsTable
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsTitle
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsToolbar
import javax.inject.Inject

class NftDetailsFragment : BaseFragment<NftDetailsViewModel>() {

    companion object {

        private const val PAYLOAD = "NftDetailsFragment.PAYLOAD"

        fun getBundle(nftId: String) = bundleOf(PAYLOAD to nftId)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val contentViews by lazy(LazyThreadSafetyMode.NONE) {
        listOf(nftDetailsMedia, nftDetailsTitle, nftDetailsDescription, nftDetailsIssuance, nftDetailsPrice, nftDetailsTable)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_nft_details, container, false)
    }

    override fun initViews() {
        nftDetailsToolbar.applyStatusBarInsets()
        nftDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        nftDetailsOnwer.setOnClickListener { viewModel.ownerClicked() }
        nftDetailsCreator.setOnClickListener { viewModel.creatorClicked() }

        nftDetailsCollection.valuePrimary.ellipsize = TextUtils.TruncateAt.END

        nftDetailsProgress.makeVisible()
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
            nftDetailsProgress.makeGone()
            contentViews.forEach(View::makeVisible)

            nftDetailsMedia.load(it.media, imageLoader) {
                placeholder(R.drawable.nft_media_progress)
                error(R.drawable.nft_media_progress)
            }
            nftDetailsTitle.text = it.name
            nftDetailsDescription.setTextOrHide(it.description)
            nftDetailsIssuance.text = it.issuance

            nftDetailsPrice.setPriceOrHide(it.price)

            if (it.collection != null) {
                nftDetailsCollection.makeVisible()
                nftDetailsCollection.loadImage(it.collection.media)
                nftDetailsCollection.showValue(it.collection.name)
            } else {
                nftDetailsCollection.makeGone()
            }

            nftDetailsOnwer.showAddress(it.owner)

            if (it.creator != null) {
                nftDetailsCreator.makeVisible()
                nftDetailsCreator.showAddress(it.creator)
            } else {
                nftDetailsCreator.makeGone()
            }

            nftDetailsChain.showChain(it.network)
        }

        viewModel.exitingErrorLiveData.observeEvent {
            errorDialog(requireContext(), onConfirm = viewModel::backClicked) {
                setMessage(it)
            }
        }
    }
}
