package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.databinding.FragmentMyContributionsBinding
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureComponent

import javax.inject.Inject

class UserContributionsFragment : BaseFragment<UserContributionsViewModel, FragmentMyContributionsBinding>() {

    override val binder by viewBinding(FragmentMyContributionsBinding::bind)

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val headerAdapter = TotalContributionsHeaderAdapter()

    private val listAdapter by lazy(LazyThreadSafetyMode.NONE) {
        UserContributionsAdapter(imageLoader)
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(headerAdapter, listAdapter)
    }

    override fun initViews() {
        binder.myContributionsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }
        }

        binder.myContributionsList.adapter = adapter
        binder.myContributionsList.setHasFixedSize(true)

        binder.myContributionsToolbar.setHomeButtonListener { viewModel.backClicked() }
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
    }
}
