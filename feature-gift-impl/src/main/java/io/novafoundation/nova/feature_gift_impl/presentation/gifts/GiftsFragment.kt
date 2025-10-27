package io.novafoundation.nova.feature_gift_impl.presentation.gifts

import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.databinding.FragmentGiftsBinding
import io.novafoundation.nova.feature_gift_impl.di.GiftFeatureComponent
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.list.GiftsHeaderAdapter
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.list.GiftsInstructionsAdapter

class GiftsFragment : BaseFragment<GiftsViewModel, FragmentGiftsBinding>(), GiftsHeaderAdapter.ItemHandler {

    companion object : PayloadCreator<GiftsPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentGiftsBinding.inflate(layoutInflater)

    private val headerAdapter = GiftsHeaderAdapter(this)

    private val instructionsAdapter = GiftsInstructionsAdapter()

    // TODO: list adapter for gifts

    private val adapter = ConcatAdapter(headerAdapter, instructionsAdapter)

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
        }
    }

    override fun onLearnMoreClicked() {
        viewModel.learnMoreClicked()
    }
}
