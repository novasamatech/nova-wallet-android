package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import androidx.core.os.bundleOf
import io.novafoundation.nova.common.view.showLoadingValue
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.ExecutionTimerView
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorExitReviewBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeResultBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

class BittensorExitReviewFragment : BaseFragment<BittensorExitReviewViewModel, FragmentBittensorExitReviewBinding>() {

    companion object {

        private const val PAYLOAD = "BittensorExitReviewFragment.Payload"

        fun getBundle(payload: BittensorExitReviewPayload) = bundleOf(PAYLOAD to payload)
    }

    private var resultSheet: BittensorStakeResultBottomSheet? = null

    override fun createBinding() = FragmentBittensorExitReviewBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorExitReviewComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setTitle(viewModel.title)
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        primaryAmount.isVisible = viewModel.isRoot
        assets.isVisible = !viewModel.isRoot
        stakingType.isVisible = viewModel.isRoot
        stakeAfter.isVisible = viewModel.isRoot
        rate.isVisible = !viewModel.isRoot
        averagePrice.isVisible = !viewModel.isRoot
        earned.isVisible = !viewModel.isRoot
        slippage.isVisible = !viewModel.isRoot
        commissionCaption.isVisible = !viewModel.isRoot
        validator.image.updateLayoutParams { width = 18.dp(requireContext()); height = 18.dp(requireContext()) }
        validator.setOnClickListener { viewModel.validatorClicked() }
        information.setOnAccountClickedListener { viewModel.accountClicked() }
        execution.stepContainer.background = requireContext().getBlockDrawable()
        execution.validator.image.updateLayoutParams { width = 18.dp(requireContext()); height = 18.dp(requireContext()) }
        execution.root.setPadding(0, 0, 0, 84.dp(requireContext()))
        execution.validator.setOnClickListener { viewModel.validatorClicked() }
        execution.slippage.showValue("0.5%")
        retry.setOnClickListener { viewModel.retry() }
        confirm.prepareForProgress(viewLifecycleOwner)
        confirm.setOnClickListener { viewModel.confirmClicked() }
        done.setOnClickListener { viewModel.doneClicked() }
    }

    override fun subscribe(viewModel: BittensorExitReviewViewModel) = with(binder) {
        setupExternalActions(viewModel)
        viewModel.primaryAmount.observe { it?.let(primaryAmount::setAmount) }
        viewModel.assets.observe {
            it?.let {
                assets.setModel(it)
                execution.assets.setModel(it)
            }
        }
        viewModel.wallet.observe(information::setWallet)
        viewModel.account.observe(information::setAccount)
        viewModel.fee.observe {
            information.setFeeStatus(it)
            execution.networkFee.setFeeStatus(it)
        }
        validator.showValue(viewModel.validator)
        execution.validator.showValue(viewModel.validator)
        viewModel.averagePrice.observe(averagePrice::showLoadingValue)
        viewModel.earned.observe(earned::showLoadingValue)
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
            execution.validator.image.isVisible = it != null
            execution.validator.image.setImageDrawable(it)
        }
        viewModel.stakeAfter.observe(stakeAfter::showValue)
        viewModel.rate.observe {
            rate.showValue(it)
            execution.rate.showValue(it)
        }
        viewModel.commissionCaption.observe { commissionCaption.text = it }
        viewModel.feeCaption.observe { feeCaption.text = it }
        viewModel.confirmButtonState.observe(confirm::setState)
        viewModel.error.observe {
            error.text = it
            error.isVisible = it != null
            retry.isVisible = it != null
        }
        viewModel.showExecution.observe {
            reviewContent.isVisible = !it
            execution.root.isVisible = it
        }
        viewModel.isExecuting.observe { running ->
            if (running && !viewModel.isRoot) {
                val remaining = viewModel.remainingExecutionTime()
                execution.timer.setState(remaining?.let(ExecutionTimerView.State::CountdownTimer) ?: ExecutionTimerView.State.Waiting)
                execution.title.setText(R.string.common_do_not_close_app)
            }
        }
        viewModel.isCompleted.observe { complete ->
            done.isVisible = complete && !viewModel.isRoot
            if (complete) {
                execution.timer.setState(ExecutionTimerView.State.Success)
                execution.title.setText(R.string.common_completed)
                execution.operationLabel.setText(R.string.bittensor_exit_completed_operation)
            }
        }
        viewModel.executionSubtitle.observe { execution.subtitle.text = it }
        viewModel.resultState.observe { it?.let(::showResult) }
    }

    private fun showResult(state: BittensorStakeResultUi) {
        val sheet = resultSheet ?: BittensorStakeResultBottomSheet(
            requireContext(),
            onClose = { resultSheet?.dismiss() },
            onTryAgain = {
                resultSheet?.dismiss()
                viewModel.retry()
            },
            onDiscoverSubnets = {
                resultSheet?.dismiss()
                viewModel.doneClicked()
            },
            onViewPosition = {
                resultSheet?.dismiss()
                viewModel.doneClicked()
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
