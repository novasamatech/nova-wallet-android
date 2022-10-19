package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.model.VoterModel
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_referendum_voters.referendumVotersList
import kotlinx.android.synthetic.main.fragment_referendum_voters.referendumVotersToolbar

class ReferendumVotersFragment : BaseFragment<ReferendumVotersViewModel>(), ReferendumVotersAdapter.Handler {

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

    private val votersAdapter by lazy(LazyThreadSafetyMode.NONE) { ReferendumVotersAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_voters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun initViews() {
        referendumVotersToolbar.setTitle(viewModel.title)
        referendumVotersList.adapter = votersAdapter

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

        viewModel.votersList.observe {
            votersAdapter.submitList(it)
        }
    }

    override fun onVoterClick(voter: VoterModel) {
        viewModel.voterClicked(voter)
    }
}
