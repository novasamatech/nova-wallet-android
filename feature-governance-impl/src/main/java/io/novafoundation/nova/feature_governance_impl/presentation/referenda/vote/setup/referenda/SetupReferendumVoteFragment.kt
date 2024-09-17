package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVoteFragment
import kotlinx.android.synthetic.main.layout_setup_vote_control_aye_nay_abstain.setupReferendumVoteControlView

class SetupReferendumVoteFragment : SetupVoteFragment<SetupReferendumVoteViewModel>() {

    override fun getControlView(inflater: LayoutInflater, parent: View): View {
        return inflater.inflate(R.layout.layout_setup_vote_control_aye_nay_abstain, parent as ViewGroup, false)
    }

    override fun initViews() {
        super.initViews()

        setupReferendumVoteControlView.ayeButton.prepareForProgress(viewLifecycleOwner)
        setupReferendumVoteControlView.abstainButton.prepareForProgress(viewLifecycleOwner)
        setupReferendumVoteControlView.nayButton.prepareForProgress(viewLifecycleOwner)

        setupReferendumVoteControlView.setAyeClickListener { viewModel.ayeClicked() }
        setupReferendumVoteControlView.setAbstainClickListener { viewModel.abstainClicked() }
        setupReferendumVoteControlView.setNayClickListener { viewModel.nayClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .setupReferendumVoteFactory()
            .create(this, getPayload())
            .inject(this)
    }

    override fun subscribe(viewModel: SetupReferendumVoteViewModel) {
        super.subscribe(viewModel)

        viewModel.ayeButtonStateFlow.observe(setupReferendumVoteControlView.ayeButton::setState)
        viewModel.abstainButtonStateFlow.observe(setupReferendumVoteControlView.abstainButton::setState)
        viewModel.nayButtonStateFlow.observe(setupReferendumVoteControlView.nayButton::setState)
    }
}
