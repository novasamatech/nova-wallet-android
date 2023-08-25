package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm

import android.os.Bundle
import android.text.TextUtils.TruncateAt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import kotlinx.android.synthetic.main.fragment_start_multi_staking_confirm.startMultiStakingConfirmAmount
import kotlinx.android.synthetic.main.fragment_start_multi_staking_confirm.startMultiStakingConfirmConfirm
import kotlinx.android.synthetic.main.fragment_start_multi_staking_confirm.startMultiStakingConfirmExtrinsicInformation
import kotlinx.android.synthetic.main.fragment_start_multi_staking_confirm.startMultiStakingConfirmStakingType
import kotlinx.android.synthetic.main.fragment_start_multi_staking_confirm.startMultiStakingConfirmStakingTypeDetails
import kotlinx.android.synthetic.main.fragment_start_multi_staking_confirm.startMultiStakingConfirmToolbar

private const val PAYLOAD_KEY = "ConfirmMultiStakingFragment.PAYLOAD_KEY"

class ConfirmMultiStakingFragment : BaseFragment<ConfirmMultiStakingViewModel>() {

    companion object {

        fun getBundle(payload: ConfirmMultiStakingPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_start_multi_staking_confirm, container, false)
    }

    override fun initViews() {
        startMultiStakingConfirmToolbar.applyStatusBarInsets()

        startMultiStakingConfirmExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        startMultiStakingConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }
        startMultiStakingConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        startMultiStakingConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        startMultiStakingConfirmStakingTypeDetails.setOnClickListener { viewModel.onStakingTypeDetailsClicked() }
        startMultiStakingConfirmStakingTypeDetails.valuePrimary.ellipsize = TruncateAt.END
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmMultiStakingComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmMultiStakingViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        viewModel.showNextProgress.observe(startMultiStakingConfirmConfirm::setProgress)

        viewModel.amountModelFlow.observe(startMultiStakingConfirmAmount::setAmount)

        viewModel.feeStatusFlow.observe(startMultiStakingConfirmExtrinsicInformation::setFeeStatus)
        viewModel.walletUiFlow.observe(startMultiStakingConfirmExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(startMultiStakingConfirmExtrinsicInformation::setAccount)

        viewModel.stakingTypeModel.observe { model ->
            startMultiStakingConfirmStakingType.showValue(model.stakingTypeValue)

            with(model.stakingTypeDetails) {
                startMultiStakingConfirmStakingTypeDetails.setTitle(label)
                startMultiStakingConfirmStakingTypeDetails.showValue(value)
                startMultiStakingConfirmStakingTypeDetails.loadImage(icon)
            }
        }
    }
}
