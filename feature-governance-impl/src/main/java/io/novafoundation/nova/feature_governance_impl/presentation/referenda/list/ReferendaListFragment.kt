package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetChange
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.subscribeOnAssetClick
import kotlinx.android.synthetic.main.fragment_referenda_list.referendaList
import kotlinx.android.synthetic.main.fragment_referenda_list.referendaProgress
import javax.inject.Inject

class ReferendaListFragment : BaseFragment<ReferendaListViewModel>(), ReferendaListAdapter.Handler, ReferendaListHeaderAdapter.Handler {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val referendaHeaderAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListHeaderAdapter(imageLoader, this) }

    private val referendaListAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendaListAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referenda_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun initViews() {
        referendaList.adapter = ConcatAdapter(referendaHeaderAdapter, referendaListAdapter)
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendaListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendaListViewModel) {
        subscribeOnAssetClick(viewModel.assetSelectorMixin, imageLoader)
        subscribeOnAssetChange(viewModel.assetSelectorMixin) {
            referendaHeaderAdapter.setAsset(it)
        }

        viewModel.referendaUiFlow.observe {
            when (it) {
                is LoadingState.Loaded -> {
                    referendaProgress.makeGone()
                    referendaListAdapter.submitList(it.data)
                }
                is LoadingState.Loading -> {
                    referendaProgress.makeVisible()
                    referendaListAdapter.submitList(emptyList())
                }
            }
        }
    }

    override fun onReferendaClick(referendum: ReferendumModel) {
        viewModel.openReferendum(referendum)
    }

    override fun onClickAssetSelector() {
        viewModel.assetSelectorMixin.assetSelectorClicked()
    }
}
