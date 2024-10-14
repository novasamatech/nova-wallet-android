package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm

import android.os.Bundle
import android.text.TextUtils.TruncateAt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStartMultiStakingConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

private const val PAYLOAD_KEY = "ConfirmMultiStakingFragment.PAYLOAD_KEY"

class ConfirmMultiStakingFragment : BaseFragment<ConfirmMultiStakingViewModel, FragmentStartMultiStakingConfirmBinding>() {

    companion object {

        fun getBundle(payload: ConfirmMultiStakingPayload) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, payload)
        }
    }

    override val binder by viewBinding(FragmentStartMultiStakingConfirmBinding::bind)

    override fun initViews() {
        binder.startMultiStakingConfirmToolbar.applyStatusBarInsets()

        binder.startMultiStakingConfirmExtrinsicInformation.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.startMultiStakingConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.startMultiStakingConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        binder.startMultiStakingConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.startMultiStakingConfirmStakingTypeDetails.setOnClickListener { viewModel.onStakingTypeDetailsClicked() }
        binder.startMultiStakingConfirmStakingTypeDetails.valuePrimary.ellipsize = TruncateAt.END
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

        viewModel.showNextProgress.observe(binder.startMultiStakingConfirmConfirm::setProgressState)

        viewModel.amountModelFlow.observe(binder.startMultiStakingConfirmAmount::setAmount)

        viewModel.feeStatusFlow.observe(binder.startMultiStakingConfirmExtrinsicInformation::setFeeStatus)
        viewModel.walletUiFlow.observe(binder.startMultiStakingConfirmExtrinsicInformation::setWallet)
        viewModel.originAddressModelFlow.observe(binder.startMultiStakingConfirmExtrinsicInformation::setAccount)

        viewModel.stakingTypeModel.observe { model ->
            binder.startMultiStakingConfirmStakingType.showValue(model.stakingTypeValue)

            with(model.stakingTypeDetails) {
                binder.startMultiStakingConfirmStakingTypeDetails.setTitle(label)
                binder.startMultiStakingConfirmStakingTypeDetails.showValue(value)
                binder.startMultiStakingConfirmStakingTypeDetails.loadImage(icon)
            }
        }
    }
}
