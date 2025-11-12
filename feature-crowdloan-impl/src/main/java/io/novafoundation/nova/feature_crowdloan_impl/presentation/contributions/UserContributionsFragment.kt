package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import androidx.recyclerview.widget.ConcatAdapter

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.databinding.FragmentMyContributionsBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent

import javax.inject.Inject

class UserContributionsFragment : BaseFragment<UserContributionsViewModel, FragmentMyContributionsBinding>() {

    override fun createBinding() = FragmentMyContributionsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val headerAdapter = TotalContributionsHeaderAdapter()

    private val listAdapter by lazy(LazyThreadSafetyMode.NONE) {
        UserContributionsAdapter(imageLoader)
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(headerAdapter, listAdapter)
    }

    override fun initViews() {
        binder.myContributionsList.adapter = adapter
        binder.myContributionsList.setHasFixedSize(true)

        binder.myContributionsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.myContributionsClaim.setOnClickListener { viewModel.claimClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<CrowdloanFeatureComponent>(
            requireContext(),
            CrowdloanFeatureApi::class.java
        )
            .userContributionsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: UserContributionsViewModel) {
        viewModel.totalContributedAmountFlow.observe {
            headerAdapter.setAmount(it)
        }

        viewModel.contributionModelsFlow.observe { loadingState ->
            binder.myContributionsList.setVisible(loadingState is LoadingState.Loaded && loadingState.data.isNotEmpty())
            binder.myContributionsPlaceholder.setVisible(loadingState is LoadingState.Loaded && loadingState.data.isEmpty())
            binder.myContributionsProgress.setVisible(loadingState is LoadingState.Loading)

            if (loadingState is LoadingState.Loaded) {
                listAdapter.submitList(loadingState.data)
            }
        }

        viewModel.claimContributionsVisible.observe(binder.myContributionsClaim::setVisible)
    }
}
