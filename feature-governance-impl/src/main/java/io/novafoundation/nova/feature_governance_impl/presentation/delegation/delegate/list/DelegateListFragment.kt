package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.ExtendedLoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import kotlinx.android.synthetic.main.fragment_delegate_list.delegateListList
import kotlinx.android.synthetic.main.fragment_delegate_list.delegateListProgress
import javax.inject.Inject

class DelegateListFragment : BaseFragment<DelegateListViewModel>(), DelegateListAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val delegateListAdapter by lazy(LazyThreadSafetyMode.NONE) { DelegateListAdapter(imageLoader, this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_delegate_list, container, false)
    }

    override fun initViews() {
        delegateListList.itemAnimator = null
        delegateListList.setHasFixedSize(true)
        delegateListList.adapter = delegateListAdapter
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .delegateListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: DelegateListViewModel) {
        viewModel.delegateModels.observe {
            when(it) {
                is ExtendedLoadingState.Error -> { }
                is ExtendedLoadingState.Loaded -> {
                    delegateListList.makeVisible()
                    delegateListProgress.makeGone()

                    delegateListAdapter.submitList(it.data)
                }
                ExtendedLoadingState.Loading -> {
                    delegateListList.makeGone()
                    delegateListProgress.makeVisible()
                }
            }
        }
    }

    override fun itemClicked(position: Int) {
        viewModel.delegateClicked(position)
    }


}
