package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list.VoterItemDecoration
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list.VotersAdapter

import javax.inject.Inject

class ReferendumVotersFragment : BaseFragment<ReferendumVotersViewModel>(), VotersAdapter.Handler {

    companion object {
        private const val KEY_PAYLOAD = "payload"

        fun getBundle(payload: ReferendumVotersPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val votersAdapter by lazy(LazyThreadSafetyMode.NONE) { VotersAdapter(this, imageLoader) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_voters, container, false)
    }

    override fun initViews() {
        referendumVotersToolbar.setTitle(viewModel.title)
        referendumVotersList.setHasFixedSize(true)
        referendumVotersList.adapter = votersAdapter
        referendumVotersList.addItemDecoration(VoterItemDecoration(requireContext(), votersAdapter))
        referendumVotersToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .referendumVotersFactory()
            .create(this, arguments!!.getParcelable(KEY_PAYLOAD)!!)
            .inject(this)
    }

    override fun subscribe(viewModel: ReferendumVotersViewModel) {
        setupExternalActions(viewModel)

        viewModel.voterModels.observe {
            if (it is LoadingState.Loaded) {
                val voters = it.data
                votersAdapter.submitList(voters)
                referendumVotersPlaceholder.isVisible = voters.isEmpty()
                referendumVotersList.isVisible = voters.isNotEmpty()
                referendumVotersCount.makeVisible()
                referendumVotersProgress.makeGone()
            } else {
                referendumVotersPlaceholder.makeGone()
                referendumVotersProgress.makeVisible()
            }
        }

        viewModel.votersCount.observe(referendumVotersCount::setText)
    }

    override fun onVoterClick(position: Int) {
        viewModel.voterClicked(position)
    }

    override fun onExpandItemClick(position: Int) {
        viewModel.expandVoterClicked(position)
    }
}
