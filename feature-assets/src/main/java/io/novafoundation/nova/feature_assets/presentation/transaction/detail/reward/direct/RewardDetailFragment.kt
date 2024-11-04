package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.direct

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddressOrHide
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_assets.databinding.FragmentRewardSlashDetailsBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_assets.presentation.model.toAmountModel

class RewardDetailFragment : BaseFragment<RewardDetailViewModel, FragmentRewardSlashDetailsBinding>() {

    companion object {
        private const val KEY_REWARD = "KEY_REWARD"

        fun getBundle(operation: OperationParcelizeModel.Reward) = Bundle().apply {
            putParcelable(KEY_REWARD, operation)
        }
    }

    override val binder by viewBinding(FragmentRewardSlashDetailsBinding::bind)

    override fun initViews() {
        binder.rewardDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.rewardDetailEvent.setOnClickListener {
            viewModel.eventIdClicked()
        }

        binder.rewardDetailValidator.setOnClickListener {
            viewModel.validatorAddressClicked()
        }
    }

    override fun inject() {
        val operation = argument<OperationParcelizeModel.Reward>(KEY_REWARD)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .rewardDetailComponentFactory()
            .create(this, operation)
            .inject(this)
    }

    override fun subscribe(viewModel: RewardDetailViewModel) {
        setupExternalActions(viewModel)

        with(viewModel.operation) {
            binder.rewardDetailEvent.showValue(eventId)
            binder.rewardDetailToolbar.setTitle(time.formatDateTime())
            binder.rewardDetailAmount.setAmount(amount.toAmountModel())

            binder.rewardDetailEra.showValue(era)

            binder.rewardDetailStatus.showOperationStatus(statusAppearance)

            binder.rewardDetailType.showValue(type)
        }

        viewModel.validatorAddressModelFlow.observe(binder.rewardDetailValidator::showAddressOrHide)

        viewModel.chainUi.observe(binder.rewardDetailNetwork::showChain)
    }
}
