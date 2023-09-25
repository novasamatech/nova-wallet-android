package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.pool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_assets.presentation.model.toAmountModel
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.showPool
import kotlinx.android.synthetic.main.fragment_pool_reward_details.poolRewardDetailAmount
import kotlinx.android.synthetic.main.fragment_pool_reward_details.poolRewardDetailEventId
import kotlinx.android.synthetic.main.fragment_pool_reward_details.poolRewardDetailNetwork
import kotlinx.android.synthetic.main.fragment_pool_reward_details.poolRewardDetailPool
import kotlinx.android.synthetic.main.fragment_pool_reward_details.poolRewardDetailStatus
import kotlinx.android.synthetic.main.fragment_pool_reward_details.poolRewardDetailToolbar
import kotlinx.android.synthetic.main.fragment_pool_reward_details.poolRewardDetailType

class PoolRewardDetailFragment : BaseFragment<PoolRewardDetailViewModel>() {

    companion object {
        private const val PAYLOAD_KEY = "RewardDetailFragment.PAYLOAD_KEY"

        fun getBundle(operation: OperationParcelizeModel.PoolReward) = Bundle().apply {
            putParcelable(PAYLOAD_KEY, operation)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_pool_reward_details, container, false)

    override fun initViews() {
        poolRewardDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        poolRewardDetailEventId.setOnClickListener {
            viewModel.eventIdClicked()
        }

        poolRewardDetailPool.setOnClickListener {
            viewModel.poolClicked()
        }

        poolRewardDetailStatus.showOperationStatus(OperationStatusAppearance.COMPLETED)
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
            poolRewardDetailEventId.showValueOrHide(eventId)
            poolRewardDetailToolbar.setTitle(time.formatDateTime())
            poolRewardDetailAmount.setAmount(amount.toAmountModel())

            poolRewardDetailType.showValue(type)
        }

        viewModel.poolDisplayFlow.observe(poolRewardDetailPool::showPool)

        viewModel.chainUi.observe(poolRewardDetailNetwork::showChain)
    }
}
