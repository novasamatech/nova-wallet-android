package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatDateTime
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailDate
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailEra
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailHash
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailReward
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailRewardLabel
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailToolbar
import kotlinx.android.synthetic.main.fragment_reward_slash_details.rewardDetailValidator

private const val KEY_REWARD = "KEY_REWARD"

class RewardDetailFragment : BaseFragment<RewardDetailViewModel>() {
    companion object {
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

        rewardDetailHash.setWholeClickListener {
            viewModel.eventIdClicked()
        }

        rewardDetailValidator.setWholeClickListener {
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

    private fun amountColorRes(operation: OperationParcelizeModel.Reward) = when {
        operation.isReward -> R.color.green
        else -> R.color.white
    }

    override fun subscribe(viewModel: RewardDetailViewModel) {
        setupExternalActions(viewModel)

        with(viewModel.operation) {
            rewardDetailHash.setMessage(eventId)
            rewardDetailDate.text = time.formatDateTime(requireContext())
            rewardDetailReward.text = amount
            rewardDetailReward.setTextColorRes(amountColorRes(this))

            if (isReward) {
                rewardDetailRewardLabel.setText(R.string.staking_reward)
            } else {
                rewardDetailRewardLabel.setText(R.string.staking_slash)
            }
        }

        viewModel.validatorAddressModelLiveData.observe { addressModel ->
            if (addressModel != null) {
                rewardDetailValidator.setMessage(addressModel.nameOrAddress)
                rewardDetailValidator.setTextIcon(addressModel.image)
            } else {
                rewardDetailValidator.makeGone()
            }
        }

        viewModel.eraLiveData.observe {
            rewardDetailEra.text = it
        }
    }
}
