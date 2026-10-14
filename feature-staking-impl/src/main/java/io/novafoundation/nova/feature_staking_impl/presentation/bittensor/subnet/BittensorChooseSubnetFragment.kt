package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorChooseSubnetBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent

class BittensorChooseSubnetFragment : BaseFragment<BittensorChooseSubnetViewModel, FragmentBittensorChooseSubnetBinding>() {

    private val imageLoader by lazy { FeatureUtils.getCommonApi(requireContext()).imageLoader() }

    private val adapter by lazy {
        BittensorSubnetListAdapter(imageLoader, viewModel::rootStaking, viewModel::select, viewModel::toggleFavorite)
    }

    override fun createBinding() = FragmentBittensorChooseSubnetBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetComponentFactory().create(this).inject(this)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        subnets.adapter = adapter
        toolbar.searchField.content.doAfterTextChanged { viewModel.search(it.toString()) }
        retry.setOnClickListener { viewModel.retry() }
    }

    override fun subscribe(viewModel: BittensorChooseSubnetViewModel) = with(binder) {
        viewModel.list.observe(adapter::submitList)
        viewModel.isEmpty.observe { empty.isVisible = it }
        viewModel.isLoading.observe { loading.isVisible = it }
        viewModel.hasError.observe {
            error.isVisible = it
            retry.isVisible = it
        }
    }
}
