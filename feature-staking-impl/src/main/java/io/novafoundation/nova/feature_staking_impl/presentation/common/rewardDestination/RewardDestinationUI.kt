package io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.account.chooser.AccountChooserBottomSheetDialog
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.view.RewardDestinationChooserView
import io.novafoundation.nova.feature_staking_impl.presentation.view.showRewardEstimation

fun <V> BaseFragment<V, *>.observeRewardDestinationChooser(
    viewModel: V,
    chooser: RewardDestinationChooserView,
) where V : BaseViewModel, V : RewardDestinationMixin {
    viewModel.rewardDestinationModelFlow.observe {
        chooser.payoutTitle.setVisible(it is RewardDestinationModel.Payout)
        chooser.payoutTarget.setVisible(it is RewardDestinationModel.Payout)
        chooser.destinationRestake.setChecked(it is RewardDestinationModel.Restake)
        chooser.destinationPayout.setChecked(it is RewardDestinationModel.Payout)

        if (it is RewardDestinationModel.Payout) {
            chooser.payoutTarget.setAddressModel(it.destination)
        }
    }

    viewModel.rewardReturnsLiveData.observe {
        chooser.destinationPayout.showRewardEstimation(it.payout)
        chooser.destinationRestake.showRewardEstimation(it.restake)
    }

    viewModel.showDestinationChooserEvent.observeEvent {
        AccountChooserBottomSheetDialog(
            context = requireContext(),
            payload = it,
            onSuccess = { _, item -> viewModel.payoutDestinationChanged(item) },
            onCancel = null,
            title = R.string.staking_select_payout_account
        ).show()
    }

    chooser.destinationPayout.setOnClickListener { viewModel.payoutClicked(viewModel) }
    chooser.destinationRestake.setOnClickListener { viewModel.restakeClicked() }
    chooser.payoutTarget.setActionClickListener { viewModel.payoutTargetClicked(viewModel) }
    chooser.learnMore.setOnClickListener { viewModel.learnMoreClicked() }
}
