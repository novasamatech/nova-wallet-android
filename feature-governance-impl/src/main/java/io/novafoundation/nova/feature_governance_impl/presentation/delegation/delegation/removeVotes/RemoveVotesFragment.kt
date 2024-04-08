package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackListBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_remove_votes.removeVoteConfirm
import kotlinx.android.synthetic.main.fragment_remove_votes.removeVoteExtrinsicInfo
import kotlinx.android.synthetic.main.fragment_remove_votes.removeVoteToolbar
import kotlinx.android.synthetic.main.fragment_remove_votes.removeVoteTracks

class RemoveVotesFragment : BaseFragment<RemoveVotesViewModel>() {

    companion object {

        private const val PAYLOAD = "ConfirmReferendumVoteFragment.Payload"

        fun getBundle(payload: RemoveVotesPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_remove_votes, container, false)
    }

    override fun initViews() {
        removeVoteToolbar.setHomeButtonListener { viewModel.backClicked() }

        removeVoteConfirm.prepareForProgress(viewLifecycleOwner)
        removeVoteConfirm.setOnClickListener { viewModel.confirmClicked() }

        removeVoteExtrinsicInfo.setOnAccountClickedListener { viewModel.accountClicked() }

        removeVoteTracks.setOnClickListener { viewModel.tracksClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .removeVoteFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: RemoveVotesViewModel) {
        observeValidations(viewModel)
        setupExternalActions(viewModel)

        setupFeeLoading(viewModel, removeVoteExtrinsicInfo.fee)
        viewModel.selectedAccount.observe(removeVoteExtrinsicInfo::setAccount)
        viewModel.walletModel.observe(removeVoteExtrinsicInfo::setWallet)

        viewModel.tracksModelFlow.observe {
            removeVoteTracks.showValue(it.overview)
        }

        viewModel.showNextProgress.observe(removeVoteConfirm::setProgressState)

        viewModel.showTracksEvent.observeEvent { tracks ->
            TrackListBottomSheet(
                context = requireContext(),
                data = tracks
            ).show()
        }
    }
}
