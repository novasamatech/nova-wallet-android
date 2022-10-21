package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooser
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteAmount
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteAye
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteContainer
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteLockedAmountChanges
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteLockedPeriodChanges
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteNay
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteTitle
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteToolbar
import kotlinx.android.synthetic.main.fragment_setup_vote_referendum.setupReferendumVoteVotePower

class SetupVoteReferendumFragment : BaseFragment<SetupVoteReferendumViewModel>() {

    companion object {

        private const val PAYLOAD = "SetupVoteReferendumFragment.Payload"

        fun getBundle(payload: SetupVoteReferendumPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_setup_vote_referendum, container, false)
    }

    override fun initViews() {
        setupReferendumVoteContainer.applyStatusBarInsets()

        setupReferendumVoteToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        setupReferendumVoteAye.prepareForProgress(viewLifecycleOwner)
        setupReferendumVoteAye.setOnClickListener { viewModel.ayeClicked() }

        setupReferendumVoteNay.prepareForProgress(viewLifecycleOwner)
        setupReferendumVoteNay.setOnClickListener { viewModel.nayClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .setupVoteReferendumFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SetupVoteReferendumViewModel) {
        setupAmountChooser(viewModel.amountChooserMixin, setupReferendumVoteAmount)
        observeValidations(viewModel)

        setupReferendumVoteVotePower.votePowerSeekbar.setValues(viewModel.convictionValues)
        setupReferendumVoteVotePower.votePowerSeekbar.bindTo(viewModel.selectedConvictionIndex, viewLifecycleOwner.lifecycleScope)

        viewModel.title.observe(setupReferendumVoteTitle::setText)

        viewModel.locksChangeUiFlow.observe {
            setupReferendumVoteLockedAmountChanges.setAmountChangeModel(it.amountChange)
            setupReferendumVoteLockedPeriodChanges.setAmountChangeModel(it.periodChange)
        }

        viewModel.votesFormattedFlow.observe {
            setupReferendumVoteVotePower.votePowerVotesText.text = it
        }

        viewModel.ayeButtonStateFlow.observe(setupReferendumVoteAye::setState)
        viewModel.nayButtonStateFlow.observe(setupReferendumVoteNay::setState)
    }
}
