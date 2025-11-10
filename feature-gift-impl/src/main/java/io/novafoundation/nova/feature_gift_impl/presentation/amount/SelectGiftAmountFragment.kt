package io.novafoundation.nova.feature_gift_impl.presentation.amount

import android.view.View
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.insets.applySystemBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.databinding.FragmentSelectGiftAmountBinding
import io.novafoundation.nova.feature_gift_impl.di.GiftFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.bindGetAsset

class SelectGiftAmountFragment : BaseFragment<SelectGiftAmountViewModel, FragmentSelectGiftAmountBinding>() {

    companion object : PayloadCreator<SelectGiftAmountPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentSelectGiftAmountBinding.inflate(layoutInflater)

    override fun applyInsets(rootView: View) {
        binder.root.applySystemBarInsets(imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
    }

    override fun initViews() {
        binder.giftAmountToolbar.setHomeButtonListener {
            hideKeyboard()
            viewModel.back()
        }

        binder.giftAmountContinue.prepareForProgress(this)
        binder.giftAmountContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GiftFeatureComponent>(this, GiftFeatureApi::class.java)
            .selectGiftAmountComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: SelectGiftAmountViewModel) {
        observeValidations(viewModel)
        setupAmountChooser(viewModel.amountChooserMixin, binder.giftsAmount)

        viewModel.feeMixin.setupFeeLoading(binder.giftAmountFee)

        viewModel.getAssetOptionsMixin.bindGetAsset(binder.giftAmountGetTokens)

        viewModel.chainModelFlow.observe { binder.giftAmountChain.setModel(it) }

        viewModel.continueButtonStateFlow.observe(binder.giftAmountContinue::setState)
    }
}
