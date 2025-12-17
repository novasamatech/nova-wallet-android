package io.novafoundation.nova.feature_assets.presentation.topup

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_assets.databinding.FragmentTopUpAddressBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading

class TopUpAddressFragment : BaseFragment<TopUpAddressViewModel, FragmentTopUpAddressBinding>() {

    companion object : PayloadCreator<TopUpAddressPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentTopUpAddressBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.topUpAddressToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.topUpAddressContinue.prepareForProgress(viewLifecycleOwner)
        binder.topUpAddressContinue.setOnClickListener { viewModel.nextClicked() }

        onBackPressed { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .topUpCardComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: TopUpAddressViewModel) {
        observeValidations(viewModel)

        viewModel.feeMixin.setupFeeLoading(binder.topUpCardFee)

        setupAmountChooser(viewModel.amountChooserMixin, binder.topUpAddressAmount)
        setupAddressInput(viewModel.addressInputMixin, binder.topUpAddressRecipient)

        viewModel.titleFlow.observe {
            binder.topUpCardTitle.text = it
        }

        viewModel.continueButtonState.observe(binder.topUpAddressContinue::setState)
    }
}
