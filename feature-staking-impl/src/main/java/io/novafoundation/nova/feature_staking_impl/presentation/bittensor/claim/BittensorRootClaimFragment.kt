package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import androidx.core.os.bundleOf
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingValue
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorRootClaimBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.bindBittensorStakeResults

class BittensorRootClaimFragment : BaseFragment<BittensorRootClaimViewModel, FragmentBittensorRootClaimBinding>() {
    companion object {

        private const val PAYLOAD = "BittensorRootClaimFragment.Payload"

        fun getBundle(payload: BittensorRootClaimPayload) = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentBittensorRootClaimBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorRootClaimComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        validator.image.updateLayoutParams {
            width = 18.dp(requireContext())
            height = 18.dp(requireContext())
        }
        validator.setOnClickListener { viewModel.validatorClicked() }
        information.setOnAccountClickedListener { viewModel.accountClicked() }
        confirm.prepareForProgress(viewLifecycleOwner)
        confirm.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun subscribe(viewModel: BittensorRootClaimViewModel) = with(binder) {
        setupExternalActions(viewModel)
        observeBittensorRetries(viewModel.retryEvents)
        observeRetries(viewModel.feeLoaderMixin)
        observeValidations(viewModel)
        viewModel.feeCaption.observe { feeCaption.text = it }
        viewModel.amount.observe(amount::showBittensorLoadingValue)
        viewModel.forfeited.observe(forfeited::showBittensorLoadingValue)
        viewModel.stakeAfterClaim.observe { state ->
            stakeAfterClaim.showBittensorLoadingValue(state)
        }
        viewModel.disclosure.observe {
            disclosure.text = it
            disclosure.isVisible = it != null
        }
        viewModel.validator.observe(validator::showBittensorLoadingValue)
        viewModel.validatorIcon.observe {
            validator.image.isVisible = it != null
            validator.image.setImageDrawable(it)
        }
        viewModel.wallet.observe(information::setWallet)
        viewModel.account.observe(information::setAccount)
        viewModel.feeStatus.observe(information::setFeeStatus)
        viewModel.remaining.observe { remaining.text = it }
        viewModel.warning.observe {
            error.text = it
            error.isVisible = it != null
        }
        viewModel.confirmButtonState.observe(confirm::setState)
        bindBittensorStakeResults(
            results = viewModel.resultState,
            onDismiss = viewModel::resultDismissed,
            onTryAgain = viewModel::resultDismissed,
            onViewPosition = viewModel::done,
            onDiscoverSubnets = viewModel::done
        )
    }
}
