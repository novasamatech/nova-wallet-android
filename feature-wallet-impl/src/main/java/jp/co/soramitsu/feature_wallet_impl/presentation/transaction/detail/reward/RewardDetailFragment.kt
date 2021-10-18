package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.reward

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import jp.co.soramitsu.common.base.BaseFragment
import jp.co.soramitsu.common.di.FeatureUtils
import jp.co.soramitsu.common.utils.formatDateTime
import jp.co.soramitsu.common.utils.makeGone
import jp.co.soramitsu.common.utils.setTextColorRes
import jp.co.soramitsu.feature_wallet_api.di.WalletFeatureApi
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.feature_wallet_impl.di.WalletFeatureComponent
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
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

        FeatureUtils.getFeature<WalletFeatureComponent>(
            requireContext(),
            WalletFeatureApi::class.java
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
