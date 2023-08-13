package io.novafoundation.nova.feature_nft_impl.presentation.nft.details

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import coil.ImageLoader
import coil.load
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.view.setPriceOrHide
import kotlinx.android.synthetic.main.fragment_nft_details.assetActionsSend
import kotlinx.android.synthetic.main.fragment_nft_details.nftAttributesTable
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
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsTokenPrice
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsToolbar
import kotlinx.android.synthetic.main.fragment_nft_details.tagsRecyclerView
import javax.inject.Inject

class NftDetailsFragment : BaseFragment<NftDetailsViewModel>() {

    companion object {

        private const val PAYLOAD = "NftDetailsFragment.PAYLOAD"

        fun getBundle(nftId: String) = bundleOf(PAYLOAD to nftId)
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        NftTagsAdapter()
    }

    private val contentViews by lazy(LazyThreadSafetyMode.NONE) {
        listOf(
            nftDetailsMedia, nftDetailsTitle, nftDetailsDescription, nftDetailsIssuance,
            nftDetailsPrice, nftDetailsTable, assetActionsSend
        )
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
        assetActionsSend.setOnClickListener { viewModel.assetActionSend() }

        nftDetailsCollection.valuePrimary.ellipsize = TextUtils.TruncateAt.END

        nftDetailsProgress.makeVisible()
        contentViews.forEach(View::makeGone)

        tagsRecyclerView.layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP).apply {
            justifyContent = JustifyContent.FLEX_START
        }
        tagsRecyclerView.adapter = adapter
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
                error(R.drawable.nft_media_error)
                fallback(R.drawable.nft_media_error)
            }
            adapter.submitList(it.tags)

            if (it.attributes.isEmpty()) {
                nftAttributesTable.makeGone()
            } else {
                nftAttributesTable.makeVisible()
            }
            nftAttributesTable.removeAllViews()
            it.attributes.forEach {
                nftAttributesTable.addView(
                    TableCellView(nftAttributesTable.context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setTitle(it.label)
                        showValue(it.value)
                    }
                )
            }

            nftDetailsTitle.text = it.name
            nftDetailsDescription.setTextOrHide(it.description)

            nftDetailsPrice.setPriceOrHide(it.price)
            if (it.price == null) {
                nftDetailsTokenPrice.showValue(getString(R.string.nft_price_not_listed))
            } else {
                nftDetailsTokenPrice.showValue(it.price.token, it.price.fiat)
            }

            if (it.collection != null) {
                nftDetailsCollection.makeVisible()
                nftDetailsCollection.loadImage(it.collection.media)
                nftDetailsCollection.showValue(it.collection.name)
            } else {
                nftDetailsCollection.makeGone()
            }

            nftDetailsIssuance.showValue(it.issuance)

            nftDetailsOnwer.showAddress(it.owner)

            if (it.creator != null) {
                nftDetailsCreator.makeVisible()
                nftDetailsCreator.showAddress(it.creator)
            } else {
                nftDetailsCreator.makeGone()
            }

            assetActionsSend.makeVisible()
            nftDetailsChain.showChain(it.network)

            if (it.isSupportedForSend) {
                assetActionsSend.makeVisible()
            } else {
                assetActionsSend.makeGone()
            }
        }

        viewModel.exitingErrorLiveData.observeEvent {
            errorDialog(requireContext(), onConfirm = viewModel::backClicked) {
                setMessage(it)
            }
        }
    }
}
