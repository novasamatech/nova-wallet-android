package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.LayoutSetupVoteControlAyeNayAbstainBinding
import io.novafoundation.nova.feature_governance_impl.databinding.LayoutSetupVoteControlContinueBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVoteFragment

class SetupTinderGovVoteFragment : SetupVoteFragment<SetupTinderGovVoteViewModel>() {

    private lateinit var controlViewBinder: LayoutSetupVoteControlContinueBinding

    override fun getControlView(inflater: LayoutInflater, parent: View): View {
        controlViewBinder = LayoutSetupVoteControlContinueBinding.inflate(inflater, parent as ViewGroup, false)
        return controlViewBinder.root
    }

    override fun initViews() {
        super.initViews()
        binder.setupReferendumVoteSubtitle.text = getString(R.string.swipe_gov_vote_subtitle)

        controlViewBinder.setupTinderGovVoteContinue.setOnClickListener { viewModel.continueClicked() }
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
