package io.novafoundation.nova.feature_gift_impl.presentation.gifts

import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.databinding.FragmentGiftsBinding
import io.novafoundation.nova.feature_gift_impl.di.GiftFeatureComponent
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.list.GiftsHeaderAdapter
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.list.GiftsInstructionsAdapter
import javax.inject.Inject

class GiftsFragment : BaseFragment<GiftsViewModel, FragmentGiftsBinding>(), GiftsHeaderAdapter.ItemHandler, GiftsListAdapter.Handler {

    companion object : PayloadCreator<GiftsPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentGiftsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter = GiftsHeaderAdapter(this)

    private val instructionsAdapter = GiftsInstructionsAdapter()

    private val giftsTitleAdapter = CustomPlaceholderAdapter(R.layout.item_gifts_title)

    private val giftsAdapter by lazy(LazyThreadSafetyMode.NONE) { GiftsListAdapter(this, imageLoader) }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(
            headerAdapter,
            giftsTitleAdapter,
            giftsAdapter,
            instructionsAdapter
        )
    }

    override fun initViews() {
        binder.giftsToolbar.setHomeButtonListener { viewModel.back() }
        binder.giftsList.adapter = adapter

        binder.giftsCreate.setOnClickListener { viewModel.createGiftClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GiftFeatureComponent>(this, GiftFeatureApi::class.java)
            .giftsComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: GiftsViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.gifts.observe {
            instructionsAdapter.show(it.isEmpty())
            giftsTitleAdapter.show(it.isNotEmpty())
            giftsAdapter.submitList(it)
        }
    }

    override fun onLearnMoreClicked() {
        viewModel.learnMoreClicked()
    }

    override fun onGiftClicked(referendum: GiftRVItem) {
        viewModel.giftClicked(referendum)
    }
}
