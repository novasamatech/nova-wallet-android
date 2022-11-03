package io.novafoundation.nova.feature_crowdloan_impl.presentation.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.PlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.mixin.impl.setupCustomDialogDisplayer
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetChange
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetClick
import kotlinx.android.synthetic.main.fragment_crowdloans.crowdloanList
import javax.inject.Inject

class CrowdloanFragment : BaseFragment<CrowdloanViewModel>(), CrowdloanAdapter.Handler, CrowdloanHeaderAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val headerAdapter by lazy(LazyThreadSafetyMode.NONE) { CrowdloanHeaderAdapter(imageLoader, this) }

    private val shimmeringAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_crowdloans_shimmering) }

    private val placeholderAdapter by lazy(LazyThreadSafetyMode.NONE) { PlaceholderAdapter(R.layout.item_crowdloans_placeholder) }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        CrowdloanAdapter(imageLoader, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_crowdloans, container, false)
    }

    override fun initViews() {
        crowdloanList.itemAnimator = null
        crowdloanList.adapter = ConcatAdapter(headerAdapter, shimmeringAdapter, placeholderAdapter, adapter)
    }

    override fun inject() {
        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            requireContext(),
            CrowdloanFeatureApi::class.java
        )
            .crowdloansFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CrowdloanViewModel) {
        subscribeOnAssetClick(viewModel.assetSelectorMixin, imageLoader)
        subscribeOnAssetChange(viewModel.assetSelectorMixin) {
            headerAdapter.setAsset(it)
        }
        setupCustomDialogDisplayer(viewModel)
        observeValidations(viewModel)

        viewModel.crowdloanModelsFlow.observe { loadingState ->
            // GONE state does not trigger re-render on data change (i.e. when we want to drop outdated list)
            shimmeringAdapter.showPlaceholder(loadingState is LoadingState.Loading)

            if (loadingState is LoadingState.Loaded) {
                adapter.submitList(loadingState.data)
                placeholderAdapter.showPlaceholder(loadingState.data.isEmpty())
            } else {
                // to prevent outdated information appear for a moment between next chunk submitted and rendered
                adapter.submitList(emptyList())
                placeholderAdapter.showPlaceholder(false)
            }
        }

        viewModel.contributionsInfo.observe {
            if (it is LoadingState.Loaded) {
                headerAdapter.setContributionsInfo(it.data, false)
            } else {
                headerAdapter.setContributionsInfo(null, true)
            }
        }

        viewModel.mainDescription.observe(headerAdapter::setAboutDescription)
    }

    override fun crowdloanClicked(paraId: ParaId) {
        viewModel.crowdloanClicked(paraId)
    }

    override fun onClickAssetSelector() {
        viewModel.assetSelectorMixin.assetSelectorClicked()
    }

    override fun onClickContributionsInfo() {
        viewModel.myContributionsClicked()
    }
}
