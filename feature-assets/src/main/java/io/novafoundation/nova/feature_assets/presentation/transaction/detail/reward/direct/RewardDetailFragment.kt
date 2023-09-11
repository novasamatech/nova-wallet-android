package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.direct

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddressOrHide
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import io.novafoundation.nova.feature_assets.presentation.model.toAmountModel
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailAmount
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailEra
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailEvent
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailNetwork
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailStatus
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailToolbar
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailType
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailValidator

class RewardDetailFragment : BaseFragment<RewardDetailViewModel>() {
    companion object {
        private const val KEY_REWARD = "KEY_REWARD"

        fun getBundle(operation: OperationParcelizeModel.Reward) = Bundle().apply {
            putParcelable(KEY_REWARD, operation)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_reward_slash_details, container, false)

    override fun initViews() {
        rewardDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        rewardDetailEvent.setOnClickListener {
            viewModel.eventIdClicked()
        }

        rewardDetailValidator.setOnClickListener {
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
            rewardDetailEvent.showValue(eventId)
            rewardDetailToolbar.setTitle(time.formatDateTime())
            rewardDetailAmount.setAmount(amount.toAmountModel())

            rewardDetailEra.showValue(era)

            rewardDetailStatus.showOperationStatus(statusAppearance)

            rewardDetailType.showValue(type)
        }

        viewModel.validatorAddressModelFlow.observe(rewardDetailValidator::showAddressOrHide)

        viewModel.chainUi.observe(rewardDetailNetwork::showChain)
    }
}
