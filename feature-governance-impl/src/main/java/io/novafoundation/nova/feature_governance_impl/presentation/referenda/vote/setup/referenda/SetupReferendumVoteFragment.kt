package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.LayoutSetupVoteControlAyeNayAbstainBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVoteFragment

class SetupReferendumVoteFragment : SetupVoteFragment<SetupReferendumVoteViewModel>() {

    private lateinit var controlViewBinder: LayoutSetupVoteControlAyeNayAbstainBinding

    override fun getControlView(inflater: LayoutInflater, parent: View): View {
        controlViewBinder = LayoutSetupVoteControlAyeNayAbstainBinding.inflate(inflater, parent as ViewGroup, false)
        return controlViewBinder.root
    }

    override fun initViews() {
        super.initViews()

        controlViewBinder.setupReferendumVoteControlView.ayeButton.prepareForProgress(viewLifecycleOwner)
        controlViewBinder.setupReferendumVoteControlView.abstainButton.prepareForProgress(viewLifecycleOwner)
        controlViewBinder.setupReferendumVoteControlView.nayButton.prepareForProgress(viewLifecycleOwner)

        controlViewBinder.setupReferendumVoteControlView.setAyeClickListener { viewModel.ayeClicked() }
        controlViewBinder.setupReferendumVoteControlView.setAbstainClickListener { viewModel.abstainClicked() }
        controlViewBinder.setupReferendumVoteControlView.setNayClickListener { viewModel.nayClicked() }
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

        viewModel.ayeButtonStateFlow.observe(controlViewBinder.setupReferendumVoteControlView.ayeButton::setState)
        viewModel.abstainButtonStateFlow.observe(controlViewBinder.setupReferendumVoteControlView.abstainButton::setState)
        viewModel.nayButtonStateFlow.observe(controlViewBinder.setupReferendumVoteControlView.nayButton::setState)
    }
}
