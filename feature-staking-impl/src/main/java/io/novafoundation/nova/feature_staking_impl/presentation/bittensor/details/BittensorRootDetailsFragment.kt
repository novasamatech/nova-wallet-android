package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.feature_staking_impl.R
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorRootDetailsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeResultBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

class BittensorRootDetailsFragment : BaseFragment<BittensorRootDetailsViewModel, FragmentBittensorRootDetailsBinding>() {

    private var exitSheet: FixedListBottomSheet<BottomSheeetFixedListBinding>? = null

    private var recoverySheet: BittensorStakeResultBottomSheet? = null

    private val imageLoader by lazy { FeatureUtils.getCommonApi(requireContext()).imageLoader() }
    private val adapter by lazy { BittensorPositionAdapter(imageLoader, viewModel::positionClicked) }

    override fun createBinding() = FragmentBittensorRootDetailsBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorDetailsComponentFactory().create(this).inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        addStake.setOnClickListener { viewModel.addStake() }
        addStake.setBadge(null)
        claim.setBadge(null)
        claim.setOnClickListener { viewModel.claimClicked() }
        unstake.setBadge(null)
        unstake.setOnClickListener { viewModel.unstakeRoot() }
        validators.adapter = adapter
    }

    override fun subscribe(viewModel: BittensorRootDetailsViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        setupExternalActions(viewModel)
        viewModel.isLoading.observe { loading.isVisible = it }
        viewModel.exitChoices.observe(viewLifecycleOwner, EventObserver { choices -> showExitChoices(choices) })
        viewModel.canAddStake.observe {
            unstake.isEnabled = it
            unstake.alpha = if (it) 1f else 0.48f
            addStake.isEnabled = it
            addStake.alpha = if (it) 1f else 0.48f
        }
        viewModel.canClaim.observe { claim.isEnabled = it; claim.alpha = if (it) 1f else 0.48f }
        viewModel.receivedRootRewards.observe(receivedRewards::showValue)
        viewModel.claimableRewards.observe { rewards.showValue(it.token, it.fiat) }
        viewModel.rootStake.observe {
            stakeAmount.text = it.token
            stakeFiat.text = it.fiat
        }
        viewModel.assetIcon.observe { assetIcon.setIcon(it, imageLoader) }
        viewModel.hasRootStake.observe { activeStatus.isVisible = it }
        viewModel.validators.observe(adapter::submitList)
        viewModel.recoveryResult.observe { recovered ->
            if (recovered == null) dismissRecoverySheet() else showRecoveryResult(recovered)
        }
        viewModel.warning.observe {
            warning.text = it
            warning.isVisible = it != null
        }
    }
    private fun showExitChoices(choices: List<BittensorExitChoice>) {
        exitSheet?.dismiss()
        val configuration = FixedListBottomSheet.ViewConfiguration.default(requireContext())
        val sheet = object : FixedListBottomSheet<BottomSheeetFixedListBinding>(requireContext(), configuration) {}
        exitSheet = sheet
        sheet.setTitle(R.string.bittensor_exit_select_position)
        choices.forEach { choice ->
            val row = TableCellView.createTableCellView(requireContext())
            row.title.text = choice.address.take(6) + "…" + choice.address.takeLast(6)
            row.showValue(choice.amount.token, choice.amount.fiat)
            row.setOnClickListener { sheet.dismiss(); viewModel.selectExit(choice) }
            sheet.addItem(row)
        }
        sheet.show()
    }

    private fun showRecoveryResult(result: BittensorStakeResultUi.Recovered) {
        val sheet = recoverySheet ?: BittensorStakeResultBottomSheet(
            requireContext(),
            onClose = { recoverySheet?.dismiss() },
            onTryAgain = {},
            onDiscoverSubnets = { recoverySheet?.dismiss() },
            onViewPosition = { recoverySheet?.dismiss() }
        ).also {
            recoverySheet = it
            it.show()
        }
        sheet.setState(result)
        sheet.setOnDismissListener {
            recoverySheet = null
            viewModel.recoveryResultDismissed(result.metaId, result.transactionHash)
        }
    }

    private fun dismissRecoverySheet() {
        recoverySheet?.setOnDismissListener(null)
        recoverySheet?.dismiss()
        recoverySheet = null
    }

    override fun onDestroyView() {
        dismissRecoverySheet()
        exitSheet?.dismiss()
        exitSheet = null
        super.onDestroyView()
    }
}
