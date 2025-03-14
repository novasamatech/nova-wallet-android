package io.novafoundation.nova.feature_assets.presentation.novacard.topup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_top_up_card.topUpCardAmount
import kotlinx.android.synthetic.main.fragment_top_up_card.topUpCardContainer
import kotlinx.android.synthetic.main.fragment_top_up_card.topUpCardContinue
import kotlinx.android.synthetic.main.fragment_top_up_card.topUpCardFee
import kotlinx.android.synthetic.main.fragment_top_up_card.topUpCardRecipient
import kotlinx.android.synthetic.main.fragment_top_up_card.topUpCardTitle
import kotlinx.android.synthetic.main.fragment_top_up_card.topUpCardToolbar

private const val KEY_TOP_UP_CARD_PAYLOAD = "top_up_card_payload"

class TopUpCardFragment : BaseFragment<TopUpCardViewModel>() {

    companion object {

        fun getBundle(payload: TopUpCardPayload) = bundleOf(KEY_TOP_UP_CARD_PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_top_up_card, container, false)

    override fun initViews() {
        topUpCardContainer.applyStatusBarInsets(false)

        topUpCardToolbar.setHomeButtonListener { viewModel.backClicked() }

        topUpCardContinue.prepareForProgress(viewLifecycleOwner)
        topUpCardContinue.setOnClickListener { viewModel.nextClicked() }

        onBackPressed { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .topUpCardComponentFactory()
            .create(this, argument(KEY_TOP_UP_CARD_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: TopUpCardViewModel) {
        observeValidations(viewModel)

        viewModel.feeMixin.setupFeeLoading(topUpCardFee)

        setupAmountChooser(viewModel.amountChooserMixin, topUpCardAmount)
        setupAddressInput(viewModel.addressInputMixin, topUpCardRecipient)

        viewModel.titleFlow.observe {
            topUpCardTitle.text = it
        }

        viewModel.continueButtonState.observe(topUpCardContinue::setState)
    }
}
