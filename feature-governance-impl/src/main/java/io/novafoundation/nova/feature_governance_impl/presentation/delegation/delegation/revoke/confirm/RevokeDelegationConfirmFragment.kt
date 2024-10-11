package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.common.view.showLoadingValue
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.setVoteModelOrHide
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateLabelState
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackDelegationListBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class RevokeDelegationConfirmFragment : BaseFragment<RevokeDelegationConfirmViewModel>() {

    companion object {

        private const val PAYLOAD = "RevokeDelegationConfirmFragment.Payload"

        fun getBundle(payload: RevokeDelegationConfirmPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_revoke_delegation_confirm, container, false)
    }

    override fun initViews() {
        revokeDelegationConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        revokeDelegationConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        revokeDelegationConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        revokeDelegationConfirmDelegate.setOnClickListener { viewModel.delegateClicked() }

        revokeDelegationConfirmInformation.setOnAccountClickedListener { viewModel.accountClicked() }

        revokeDelegationConfirmTracks.setOnClickListener { viewModel.tracksClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .revokeDelegationConfirmFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: RevokeDelegationConfirmViewModel) {
        observeRetries(viewModel.partialRetriableMixin)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, revokeDelegationConfirmHints)

        setupFeeLoading(viewModel, revokeDelegationConfirmInformation.fee)
        viewModel.currentAddressModelFlow.observe(revokeDelegationConfirmInformation::setAccount)
        viewModel.walletModel.observe(revokeDelegationConfirmInformation::setWallet)

        viewModel.delegateLabelModel.observe(revokeDelegationConfirmDelegate::setDelegateLabelState)
        viewModel.tracksSummary.observe(revokeDelegationConfirmTracks::showValue)
        viewModel.userDelegation.observe(revokeDelegationConfirmDelegation::setVoteModelOrHide)

        viewModel.undelegatingPeriod.observe(revokeDelegationConfirmUndelegatingPeriod::showLoadingValue)

        viewModel.showNextProgress.observe(revokeDelegationConfirmConfirm::setProgressState)

        viewModel.showTracksEvent.observeEvent { tracks ->
            TrackDelegationListBottomSheet(requireContext(), tracks).show()
        }
    }
}
