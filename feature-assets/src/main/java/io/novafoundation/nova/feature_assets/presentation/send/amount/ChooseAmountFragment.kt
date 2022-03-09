package io.novafoundation.nova.feature_assets.presentation.send.amount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_choose_amount.chooseAmountContainer
import kotlinx.android.synthetic.main.fragment_choose_amount.chooseAmountFee
import kotlinx.android.synthetic.main.fragment_choose_amount.chooseAmountInput
import kotlinx.android.synthetic.main.fragment_choose_amount.chooseAmountNext
import kotlinx.android.synthetic.main.fragment_choose_amount.chooseAmountRecipientView
import kotlinx.android.synthetic.main.fragment_choose_amount.chooseAmountToolbar

private const val KEY_ADDRESS = "KEY_ADDRESS"
private const val KEY_ASSET_PAYLOAD = "KEY_ASSET_PAYLOAD"

class ChooseAmountFragment : BaseFragment<ChooseAmountViewModel>() {

    companion object {

        fun getBundle(recipientAddress: String, assetPayload: AssetPayload) = Bundle().apply {
            putString(KEY_ADDRESS, recipientAddress)
            putParcelable(KEY_ASSET_PAYLOAD, assetPayload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_choose_amount, container, false)

    override fun initViews() {
        chooseAmountContainer.applyStatusBarInsets()

        chooseAmountNext.prepareForProgress(viewLifecycleOwner)

        chooseAmountRecipientView.setWholeClickListener { viewModel.recipientAddressClicked() }

        chooseAmountToolbar.setHomeButtonListener { viewModel.backClicked() }

        chooseAmountNext.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .chooseAmountComponentFactory()
            .create(this, argument(KEY_ADDRESS), argument(KEY_ASSET_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ChooseAmountViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel, chooseAmountFee)
        setupAmountChooser(viewModel, chooseAmountInput)

        viewModel.recipientModelFlow.observe(chooseAmountRecipientView::setAddressModel)

        viewModel.continueButtonStateLiveData.observe(chooseAmountNext::setState)
    }
}
