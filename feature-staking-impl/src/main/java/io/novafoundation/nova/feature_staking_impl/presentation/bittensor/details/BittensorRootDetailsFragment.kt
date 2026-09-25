package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorRootDetailsBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class BittensorRootDetailsFragment : BaseFragment<BittensorDetailsViewModel, FragmentBittensorRootDetailsBinding>() {

    private val imageLoader by lazy { FeatureUtils.getCommonApi(requireContext()).imageLoader() }
    private val adapter by lazy { BittensorPositionAdapter(imageLoader, viewModel::positionClicked) }

    override fun createBinding() = FragmentBittensorRootDetailsBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorDetailsComponentFactory().create(this).inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        addStake.setOnClickListener { viewModel.addStake() }
        addStake.setBadge(null)
        unstake.setBadge(null)
        retry.setOnClickListener { viewModel.retry() }
        validators.adapter = adapter
    }

    override fun subscribe(viewModel: BittensorDetailsViewModel) = with(binder) {
        setupExternalActions(viewModel)
        viewModel.isLoading.observe { loading.isVisible = it }
        viewModel.canAddStake.observe {
            addStake.isEnabled = it
            addStake.alpha = if (it) 1f else 0.48f
        }
        viewModel.rootStake.observe {
            stakeAmount.text = it.token
            stakeFiat.text = it.fiat
        }
        viewModel.assetIcon.observe { assetIcon.setIcon(it, imageLoader) }
        viewModel.hasRootStake.observe { activeStatus.isVisible = it }
        viewModel.validators.observe(adapter::submitList)
        viewModel.warning.observe {
            warning.text = it
            warning.isVisible = it != null
            retry.isVisible = it != null
        }
    }
}
