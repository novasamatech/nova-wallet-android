package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.pool

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_assets.databinding.FragmentPoolRewardDetailsBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_assets.presentation.model.toAmountModel
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.showPool

class PoolRewardDetailFragment : BaseFragment<PoolRewardDetailViewModel, FragmentPoolRewardDetailsBinding>() {

    companion object {
        private const val PAYLOAD_KEY = "RewardDetailFragment.PAYLOAD_KEY"

        fun getBundle(operation: OperationParcelizeModel.PoolReward) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, operation)
        }
    }

    override fun createBinding() = FragmentPoolRewardDetailsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.poolRewardDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.poolRewardDetailEventId.setOnClickListener {
            viewModel.eventIdClicked()
        }

        binder.poolRewardDetailPool.setOnClickListener {
            viewModel.poolClicked()
        }

        binder.poolRewardDetailStatus.showOperationStatus(OperationStatusAppearance.COMPLETED)
    }

    override fun inject() {
        val operation = argument<OperationParcelizeModel.PoolReward>(PAYLOAD_KEY)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .poolRewardDetailComponentFactory()
            .create(this, operation)
            .inject(this)
    }

    override fun subscribe(viewModel: PoolRewardDetailViewModel) {
        setupExternalActions(viewModel)

        with(viewModel.operation) {
            binder.poolRewardDetailEventId.showValueOrHide(eventId)
            binder.poolRewardDetailToolbar.setTitle(time.formatDateTime())
            binder.poolRewardDetailAmount.setAmount(amount.toAmountModel())

            binder.poolRewardDetailType.showValue(type)
        }

        viewModel.poolDisplayFlow.observe(binder.poolRewardDetailPool::showPool)

        viewModel.chainUi.observe(binder.poolRewardDetailNetwork::showChain)
    }
}
