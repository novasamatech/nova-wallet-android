package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorRootClaimBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeResultBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

class BittensorRootClaimFragment : BaseFragment<BittensorRootClaimViewModel, FragmentBittensorRootClaimBinding>() {
    private var resultSheet: BittensorStakeResultBottomSheet? = null

    override fun createBinding() = FragmentBittensorRootClaimBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorRootClaimComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        validator.image.updateLayoutParams { width = 18.dp(requireContext()); height = 18.dp(requireContext()) }
        validator.setOnClickListener { viewModel.validatorClicked() }
        information.setOnAccountClickedListener { viewModel.accountClicked() }
        retry.setOnClickListener { viewModel.retry() }
        confirm.prepareForProgress(viewLifecycleOwner)
        confirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun subscribe(viewModel: BittensorRootClaimViewModel) = with(binder) {
        setupExternalActions(viewModel)
        viewModel.feeCaption.observe { feeCaption.text = it }
        viewModel.amount.observe(amount::showValue)
        viewModel.forfeited.observe(forfeited::showValue)
        viewModel.validator.observe(validator::showValue)
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
        }
        viewModel.wallet.observe(information::setWallet)
        viewModel.account.observe(information::setAccount)
        viewModel.feeStatus.observe(information::setFeeStatus)
        viewModel.remaining.observe { remaining.text = it }
        viewModel.error.observe {
            error.text = it
            error.isVisible = it != null
            retry.isVisible = it != null
        }
        viewModel.confirmButtonState.observe(confirm::setState)
        viewModel.resultState.observe { it?.let(::showResult) }
    }

    private fun showResult(state: BittensorStakeResultUi) {
        val sheet = resultSheet ?: BittensorStakeResultBottomSheet(
            requireContext(),
            onClose = { resultSheet?.dismiss() },
            onTryAgain = {
                resultSheet?.dismiss()
                viewModel.confirmClicked()
            },
            onDiscoverSubnets = {
                resultSheet?.dismiss()
                viewModel.done()
            },
            onViewPosition = {
                resultSheet?.dismiss()
                viewModel.done()
            }
        ).also {
            resultSheet = it
            it.setOnDismissListener {
                resultSheet = null
                viewModel.resultDismissed()
            }
            it.show()
        }
        sheet.setState(state)
    }

    override fun onDestroyView() {
        resultSheet?.setOnDismissListener(null)
        resultSheet?.dismiss()
        resultSheet = null
        super.onDestroyView()
    }
}
