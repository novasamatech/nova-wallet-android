package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorStakeConfirmBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi

class BittensorStakeConfirmFragment : BaseFragment<BittensorStakeConfirmViewModel, FragmentBittensorStakeConfirmBinding>() {

    private var resultBottomSheet: BittensorStakeResultBottomSheet? = null

    override fun createBinding() = FragmentBittensorStakeConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.bittensorStakeConfirmButton.prepareForProgress(viewLifecycleOwner)
        binder.bittensorStakeConfirmToolbar.setHomeButtonListener { viewModel.back() }
        binder.bittensorStakeConfirmExtrinsicInformation.setOnAccountClickedListener { viewModel.accountClicked() }
        binder.bittensorStakeConfirmValidator.setOnClickListener { viewModel.validatorClicked() }
        binder.bittensorStakeConfirmButton.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorStakeConfirmComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BittensorStakeConfirmViewModel) {
        setupExternalActions(viewModel)

        viewModel.amount.observe(binder.bittensorStakeConfirmAmount::setAmount)
        viewModel.wallet.observe(binder.bittensorStakeConfirmExtrinsicInformation::setWallet)
        viewModel.account.observe(binder.bittensorStakeConfirmExtrinsicInformation::setAccount)
        viewModel.fee.observe(binder.bittensorStakeConfirmExtrinsicInformation::setFeeStatus)
        viewModel.stakingType.observe(binder.bittensorStakeConfirmStakingType::showValue)
        viewModel.validator.observe(::showValidator)
        viewModel.stakeAfter.observe(binder.stakeAfter::showValue)
        viewModel.apy.observe(binder.bittensorStakeConfirmApy::showValue)
        viewModel.confirmButtonState.observe(binder.bittensorStakeConfirmButton::setState)
        viewModel.resultState.observe { it?.let(::showResult) }
    }

    private fun showValidator(validator: BittensorValidatorUi) {
        binder.bittensorStakeConfirmValidator.showValue(validator.name)
        validator.icon?.let { binder.bittensorStakeConfirmValidator.loadImage(it) }
            ?: binder.bittensorStakeConfirmValidator.setImage(R.drawable.ic_nova, 20)
    }

    private fun showResult(state: BittensorStakeResultUi) {
        val bottomSheet = resultBottomSheet ?: createResultBottomSheet().also {
            resultBottomSheet = it
            it.show()
        }

        bottomSheet.setState(state)
    }

    private fun createResultBottomSheet(): BittensorStakeResultBottomSheet {
        return BittensorStakeResultBottomSheet(
            context = requireContext(),
            onClose = ::dismissResult,
            onTryAgain = viewModel::retryClicked,
            onDiscoverSubnets = ::dismissResult,
            onViewPosition = {
                dismissResult()
                viewModel.viewPositionClicked()
            }
        ).also { bottomSheet ->
            bottomSheet.setOnDismissListener {
                resultBottomSheet = null
                viewModel.resultDismissed()
            }
        }
    }

    private fun dismissResult() {
        resultBottomSheet?.dismiss()
    }

    override fun onDestroyView() {
        resultBottomSheet?.setOnDismissListener(null)
        resultBottomSheet?.dismiss()
        resultBottomSheet = null
        super.onDestroyView()
    }
}
