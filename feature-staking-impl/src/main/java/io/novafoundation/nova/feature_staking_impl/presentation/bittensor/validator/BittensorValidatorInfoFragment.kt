package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import android.os.Bundle
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorValidatorInfoBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class BittensorValidatorInfoFragment : BaseFragment<BittensorValidatorInfoViewModel, FragmentBittensorValidatorInfoBinding>() {

    companion object {
        private const val PAYLOAD_KEY = "payload"

        fun getBundle(payload: BittensorValidatorInfoPayload) = Bundle().apply { putParcelable(PAYLOAD_KEY, payload) }
    }

    override fun createBinding() = FragmentBittensorValidatorInfoBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorValidatorInfoComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        account.setOnClickListener { viewModel.accountClicked() }
    }

    override fun subscribe(viewModel: BittensorValidatorInfoViewModel) = with(binder) {
        setupExternalActions(viewModel)
        viewModel.account.observe(account::setAddressModel)
        status.showValue(viewModel.status.text)
        status.setPrimaryValueStyle(viewModel.status.style)
        take.showValue(viewModel.take)
        reward.showProgress()
        viewModel.reward.observe {
            reward.showValue(it.value, it.detail)
            reward.setPrimaryValueStyle(it.style)
        }
        totalStake.showProgress()
        viewModel.totalStake.observe { amount ->
            if (amount == null) totalStake.showValue("—") else totalStake.showAmount(amount)
        }
        viewModel.coverage.observe(coverage::showBittensorLoadingValue)
    }
}
