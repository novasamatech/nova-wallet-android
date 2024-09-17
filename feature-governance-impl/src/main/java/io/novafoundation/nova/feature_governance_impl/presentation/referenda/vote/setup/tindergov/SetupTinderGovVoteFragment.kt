package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVoteFragment
import kotlinx.android.synthetic.main.fragment_setup_vote.setupReferendumVoteSubtitle
import kotlinx.android.synthetic.main.layout_setup_vote_control_continue.setupTinderGovVoteContinue

class SetupTinderGovVoteFragment : SetupVoteFragment<SetupTinderGovVoteViewModel>() {

    override fun getControlView(inflater: LayoutInflater, parent: View): View {
        return inflater.inflate(R.layout.layout_setup_vote_control_continue, parent as ViewGroup, false)
    }

    override fun initViews() {
        super.initViews()
        setupReferendumVoteSubtitle.text = getString(R.string.tinder_gov_vote_subtitle)

        setupTinderGovVoteContinue.setOnClickListener { viewModel.continueClicked() }
        onBackPressed { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .setupTinderGovVoteFactory()
            .create(this, getPayload())
            .inject(this)
    }
}
