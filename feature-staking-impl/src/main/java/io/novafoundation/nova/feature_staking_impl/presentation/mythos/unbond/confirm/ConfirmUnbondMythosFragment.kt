package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm

import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentMythosUnbondConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class ConfirmUnbondMythosFragment : BaseFragment<ConfirmUnbondMythosViewModel, FragmentMythosUnbondConfirmBinding>() {

    companion object {

        private const val PAYLOAD = "ConfirmUnbondMythosFragment.Payload"

        fun getBundle(payload: ConfirmUnbondMythosPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentMythosUnbondConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.mythosUnbondConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.mythosUnbondConfirmExtrinsicInfo.setOnAccountClickedListener { viewModel.originAccountClicked() }

        binder.mythosUnbondConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        binder.mythosUnbondConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.mythosUnbondConfirmCollator.setOnClickListener { viewModel.collatorClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .confirmUnbondMythosFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmUnbondMythosViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        setupFeeLoading(viewModel.feeLoaderMixin, binder.mythosUnbondConfirmExtrinsicInfo.fee)

        viewModel.showNextProgress.observe(binder.mythosUnbondConfirmConfirm::setProgressState)

        viewModel.currentAccountModelFlow.observe(binder.mythosUnbondConfirmExtrinsicInfo::setAccount)
        viewModel.walletFlow.observe(binder.mythosUnbondConfirmExtrinsicInfo::setWallet)

        viewModel.collatorAddressModel.observe(binder.mythosUnbondConfirmCollator::showAddress)
        viewModel.amountModel.observe(binder.mythosUnbondConfirmAmount::setAmount)
    }
}
