package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorRootDetailsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.showBittensorLoadingAmount

class BittensorRootDetailsFragment : BaseFragment<BittensorRootDetailsViewModel, FragmentBittensorRootDetailsBinding>() {

    private val imageLoader by lazy {
        FeatureUtils.getCommonApi(requireContext())
            .imageLoader()
    }

    private val adapter by lazy { BittensorPositionAdapter(imageLoader, viewModel::positionClicked) }

    override fun createBinding() = FragmentBittensorRootDetailsBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorDetailsComponentFactory()
            .create(this)
            .inject(this)
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
        rewards.valueSecondary.setTextColorRes(R.color.text_secondary)
    }

    override fun subscribe(viewModel: BittensorRootDetailsViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        setupExternalActions(viewModel)
        viewModel.isLoading.observe {
            loading.isVisible = it
            stakeAmount.isVisible = !it
            stakeFiat.isVisible = !it
        }
        viewModel.canAddStake.observe {
            addStake.isEnabled = it
            addStake.alpha = if (it) 1f else 0.48f
        }
        viewModel.canUnstake.observe {
            unstake.isEnabled = it
            unstake.alpha = if (it) 1f else 0.48f
        }
        viewModel.rootHold.observe { hold ->
            rootHoldCard.isVisible = hold != null
            holdTimeLeft.text = hold?.timeLeft
            holdDescription.text = hold?.description
        }
        viewModel.canClaim.observe { canClaim ->
            claim.isEnabled = canClaim
            claim.alpha = if (canClaim) 1f else 0.48f
        }
        viewModel.claimDetail.observe(claim::setDetail)
        viewModel.claimMinimum.observe(claimMinimum::setTextOrHide)
        viewModel.redeemableRewards.observe(rewards::showBittensorLoadingAmount)
        viewModel.rootStake.observe {
            stakeAmount.text = it.token
            stakeFiat.text = it.fiat
        }
        viewModel.assetIcon.observe { assetIcon.setIcon(it, imageLoader) }
        viewModel.isActive.observe { activeStatus.isVisible = it }
        viewModel.validators.observe(adapter::submitList)
        viewModel.warning.observe {
            warning.text = it
            warning.isVisible = it != null
        }
    }
}
