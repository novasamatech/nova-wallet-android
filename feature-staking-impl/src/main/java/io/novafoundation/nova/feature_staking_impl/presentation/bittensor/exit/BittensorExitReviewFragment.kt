package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import android.text.TextUtils
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheet
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingAmount
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.ExecutionTimerView
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorExitReviewBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showFailure
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showUnconfirmed
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.bindBittensorStakeResults

class BittensorExitReviewFragment : BaseFragment<BittensorExitReviewViewModel, FragmentBittensorExitReviewBinding>() {

    companion object {

        private const val PAYLOAD = "BittensorExitReviewFragment.Payload"

        fun getBundle(payload: BittensorExitReviewPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentBittensorExitReviewBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorExitReviewComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setTitle(viewModel.title)
        retryExecution.setOnClickListener { viewModel.failureActionClicked() }
        execution.toolbar.setHomeButtonListener { viewModel.back() }
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        primaryAmount.isVisible = viewModel.isRoot
        information.isVisible = viewModel.isRoot
        saleWalletInformation.isVisible = !viewModel.isRoot
        networkFee.isVisible = !viewModel.isRoot
        profitCaption.isVisible = !viewModel.isRoot
        feeCaption.isVisible = viewModel.isRoot
        assets.isVisible = !viewModel.isRoot
        stakingType.isVisible = viewModel.isRoot
        stakeAfter.isVisible = viewModel.isRoot
        rate.isVisible = !viewModel.isRoot
        averagePrice.isVisible = !viewModel.isRoot
        averagePrice.valueSecondary.setTextColorRes(R.color.text_secondary)
        earned.isVisible = !viewModel.isRoot
        earned.valueSecondary.setTextColorRes(R.color.text_secondary)
        slippage.isVisible = !viewModel.isRoot
        commissionCaption.isVisible = !viewModel.isRoot
        validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        validator.setOnClickListener { viewModel.validatorClicked() }
        if (viewModel.isRoot) validator.setPrimaryValueEndIcon(R.drawable.ic_info)
        information.setOnAccountClickedListener { viewModel.accountClicked() }
        saleAccount.setOnClickListener { viewModel.accountClicked() }
        slippage.setOnClickListener { showDescription(R.string.swap_slippage_title, viewModel.slippageDescription) }
        execution.slippage.setOnClickListener { showDescription(R.string.swap_slippage_title, viewModel.slippageDescription) }
        networkFee.setOnClickListener { showDescription(R.string.network_fee, getString(R.string.swap_network_fee_description)) }
        execution.networkFee.setOnClickListener { showDescription(R.string.network_fee, getString(R.string.swap_network_fee_description)) }
        execution.stepContainer.background = requireContext()
            .getBlockDrawable()
        execution.validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        execution.root.setPadding(
            0,
            0,
            0,
            84.dp(requireContext())
        )
        execution.validator.setOnClickListener { viewModel.validatorClicked() }
        execution.slippage.showValue(viewModel.slippage)
        slippage.showValue(viewModel.slippage)
        confirm.prepareForProgress(viewLifecycleOwner)
        confirm.setOnClickListener { viewModel.confirmClicked() }
        done.setOnClickListener { viewModel.doneClicked() }
    }

    override fun subscribe(viewModel: BittensorExitReviewViewModel) = with(binder) {
        setupExternalActions(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        observeRetries(viewModel.feeLoaderMixin)
        observeValidations(viewModel)
        viewModel.primaryAmount.observe { it?.let(primaryAmount::setAmount) }
        viewModel.assets.observe {
            it?.let {
                assets.setModel(it)
                execution.assets.setModel(it)
            }
        }
        viewModel.wallet.observe {
            information.setWallet(it)
            saleWallet.showWallet(it)
        }
        viewModel.account.observe {
            information.setAccount(it)
            saleAccount.showAddress(it)
        }
        viewModel.fee.observe {
            information.setFeeStatus(it)
            networkFee.setFeeStatus(it)
        }
        viewModel.executionFee.observe(execution.networkFee::setFeeStatus)
        viewModel.validatorName.observe {
            validator.showValue(it)
            execution.validator.showValue(it)
        }
        // A loading TableCellView makes itself visible: bind rows only where the operation shows them.
        if (!viewModel.isRoot) {
            viewModel.averagePrice.observe(averagePrice::showBittensorLoadingAmount)
            viewModel.earned.observe { state ->
                earned.showBittensorLoadingAmount(state.map { it.amount })
                earned.valuePrimary.setTextColorRes(state.dataOrNull?.colorRes ?: R.color.text_secondary)
            }
        }
        viewModel.swapRateDescription.observe { description ->
            rate.setOnClickListener { description?.let { showDescription(R.string.bittensor_exchange_rate, it) } }
            execution.rate.setOnClickListener { description?.let { showDescription(R.string.bittensor_exchange_rate, it) } }
        }
        viewModel.averagePriceDescription.observe { description ->
            averagePrice.setOnClickListener { description?.let { showDescription(R.string.bittensor_exit_avg_buy_price, it) } }
        }
        viewModel.profitDescription.observe { description ->
            earned.setOnClickListener { description?.let { showDescription(R.string.bittensor_you_will_earn, it) } }
        }
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
            execution.validator.image.isVisible = it != null
            execution.validator.image.setImageDrawable(it)
        }
        viewModel.isMultiValidator.observe { multiValidator ->
            validator.isVisible = !multiValidator
            execution.validator.isVisible = !multiValidator
        }
        viewModel.legs.observe(::showLegs)
        if (viewModel.isRoot) {
            viewModel.stakeAfter.observe(stakeAfter::showBittensorLoadingValue)
        } else {
            viewModel.rate.observe {
                rate.showBittensorLoadingValue(it)
                execution.rate.showBittensorLoadingValue(it)
            }
        }
        viewModel.commissionCaption.observe { commissionCaption.text = it }
        viewModel.feeCaption.observe { feeCaption.text = it }
        viewModel.confirmButtonState.observe(confirm::setState)
        viewModel.isPriceMoved.observe { priceMoved.isVisible = it }
        viewModel.showExecution.observe {
            reviewContent.isVisible = !it
            execution.root.isVisible = it
        }
        viewModel.isExecuting.observe { running ->
            if (running && !viewModel.isRoot) {
                val remaining = viewModel.remainingExecutionTime()
                execution.timer.setState(ExecutionTimerView.State.CountdownTimer(remaining))
                execution.title.setText(R.string.common_do_not_close_app)
                execution.operationLabel.setText(R.string.bittensor_subnet_execution_operation)
            }
        }
        viewModel.isCompleted.observe { complete ->
            if (complete) {
                execution.timer.setState(ExecutionTimerView.State.Success)
                execution.title.setText(R.string.common_completed)
            }
        }
        viewModel.executionSubtitle.observe { execution.subtitle.text = it }
        viewModel.executionFailure.observe {
            execution.showFailure(it)
            retryExecution.isVisible = it != null
            retryExecution.setText(if (it?.canRetry == false) R.string.common_close else R.string.common_try_again)
        }
        viewModel.canFinish.observe { done.isVisible = it && !viewModel.isRoot }
        viewModel.executionUnconfirmed.observe { state ->
            state?.let {
                execution.showUnconfirmed(it)
                retryExecution.isVisible = false
            }
        }
        bindBittensorStakeResults(viewModel.resultState, viewModel::resultActionClicked)
    }

    private fun showLegs(legs: List<BittensorExitLegUi>) = with(binder) {
        validators.removeViews(1, validators.childCount - 1)
        legs.forEach { leg ->
            val row = TableCellView.createTableCellView(requireContext())
            row.setTitleEllipsisable(true)
            row.title.ellipsize = TextUtils.TruncateAt.END
            row.title.maxLines = 1
            row.title.text = leg.name
            row.showValue(leg.amount, leg.detail)
            validators.addView(row)
        }
        validators.invalidateChildrenVisibility()
    }

    private fun showDescription(titleRes: Int, description: String) {
        DescriptionBottomSheet(requireContext(), getString(titleRes), description).show()
    }
}
