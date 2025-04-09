package io.novafoundation.nova.feature_assets.presentation.topup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_top_up_address.topUpAddressAmount
import kotlinx.android.synthetic.main.fragment_top_up_address.topUpAddressContainer
import kotlinx.android.synthetic.main.fragment_top_up_address.topUpAddressContinue
import kotlinx.android.synthetic.main.fragment_top_up_address.topUpCardFee
import kotlinx.android.synthetic.main.fragment_top_up_address.topUpAddressRecipient
import kotlinx.android.synthetic.main.fragment_top_up_address.topUpCardTitle
import kotlinx.android.synthetic.main.fragment_top_up_address.topUpAddressToolbar

class TopUpAddressFragment : BaseFragment<TopUpAddressViewModel>() {

    companion object : PayloadCreator<TopUpAddressPayload> by FragmentPayloadCreator()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_top_up_address, container, false)

    override fun initViews() {
        topUpAddressContainer.applyStatusBarInsets(false)

        topUpAddressToolbar.setHomeButtonListener { viewModel.backClicked() }

        topUpAddressContinue.prepareForProgress(viewLifecycleOwner)
        topUpAddressContinue.setOnClickListener { viewModel.nextClicked() }

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

        viewModel.feeMixin.setupFeeLoading(topUpCardFee)

        setupAmountChooser(viewModel.amountChooserMixin, topUpAddressAmount)
        setupAddressInput(viewModel.addressInputMixin, topUpAddressRecipient)

        viewModel.titleFlow.observe {
            topUpCardTitle.text = it
        }

        viewModel.continueButtonState.observe(topUpAddressContinue::setState)
    }
}
