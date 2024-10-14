package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentVotedReferendaBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list.BaseReferendaListFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel

class VotedReferendaFragment : BaseReferendaListFragment<VotedReferendaViewModel, FragmentVotedReferendaBinding>(), ReferendaListAdapter.Handler {

    companion object {
        private const val KEY_PAYLOAD = "VotedReferendaFragment.Payload"

        fun getBundle(payload: VotedReferendaPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentVotedReferendaBinding::bind)

    override fun initViews() {
        viewModel.payload.overriddenTitle?.let { binder.votedReferendaToolbar.setTitle(it) }
        binder.votedReferendaToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.votedReferendaToolbar.applyStatusBarInsets()

        binder.votedReferendaList.itemAnimator = null
        binder.votedReferendaList.adapter = ConcatAdapter(shimmeringAdapter, placeholderAdapter, referendaListAdapter)
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .votedReferendaFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: VotedReferendaViewModel) {
        viewModel.referendaUiFlow.observeReferendaList()

        viewModel.votedReferendaCount.observeWhenVisible {
            binder.votedReferendaCount.makeVisible()
            binder.votedReferendaCount.text = it
        }
    }

    override fun onReferendaClick(referendum: ReferendumModel) {
        viewModel.openReferendum(referendum)
    }
}
