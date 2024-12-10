package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_account_api.presenatation.actions.CustomizableExternalActionsSheet
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActionModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.copyAddressClicked
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStakingProxyListBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.model.StakingProxyRvItem
import javax.inject.Inject

class StakingProxyListFragment : BaseFragment<StakingProxyListViewModel, FragmentStakingProxyListBinding>(), StakingProxyListAdapter.Handler {

    override fun createBinding() = FragmentStakingProxyListBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { StakingProxyListAdapter(this, imageLoader) }

    override fun initViews() {
        binder.stakingProxyListToolbar.applyStatusBarInsets()
        binder.stakingProxyListToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.stakingProxyListAddProxyButton.setOnClickListener { viewModel.addProxyClicked() }

        binder.stakingProxyList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .stakingProxyListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StakingProxyListViewModel) {
        setupExternalActions(viewModel) { context, payload ->
            CustomizableExternalActionsSheet(
                context,
                payload,
                onCopy = viewModel::copyAddressClicked,
                onViewExternal = viewModel::viewExternalClicked,
                additionalOptions = rewokeAccessExternalAction(payload)
            )
        }

        viewModel.proxyModels.observe {
            adapter.submitList(it)
        }
    }

    override fun onProxyClick(item: StakingProxyRvItem) {
        viewModel.proxyClicked(item)
    }

    private fun rewokeAccessExternalAction(payload: ExternalActions.Payload): List<ExternalActionModel> {
        return listOf(
            ExternalActionModel(
                R.drawable.ic_delete,
                getString(R.string.common_proxy_rewoke_access),
                onClick = { viewModel.rewokeAccess(payload) }
            )
        )
    }
}
