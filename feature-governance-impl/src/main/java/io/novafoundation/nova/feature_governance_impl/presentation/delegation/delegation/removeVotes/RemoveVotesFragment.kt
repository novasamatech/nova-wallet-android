package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes

import android.os.Bundle
import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentRemoveVotesBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackListBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class RemoveVotesFragment : BaseFragment<RemoveVotesViewModel, FragmentRemoveVotesBinding>() {

    companion object {

        private const val PAYLOAD = "ConfirmReferendumVoteFragment.Payload"

        fun getBundle(payload: RemoveVotesPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentRemoveVotesBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.removeVoteToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.removeVoteConfirm.prepareForProgress(viewLifecycleOwner)
        binder.removeVoteConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.removeVoteExtrinsicInfo.setOnAccountClickedListener { viewModel.accountClicked() }

        binder.removeVoteTracks.setOnClickListener { viewModel.tracksClicked() }
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

        setupFeeLoading(viewModel, binder.removeVoteExtrinsicInfo.fee)
        viewModel.selectedAccount.observe(binder.removeVoteExtrinsicInfo::setAccount)
        viewModel.walletModel.observe(binder.removeVoteExtrinsicInfo::setWallet)

        viewModel.tracksModelFlow.observe {
            binder.removeVoteTracks.showValue(it.overview)
        }

        viewModel.showNextProgress.observe(binder.removeVoteConfirm::setProgressState)

        viewModel.showTracksEvent.observeEvent { tracks ->
            TrackListBottomSheet(
                context = requireContext(),
                data = tracks
            ).show()
        }
    }
}
