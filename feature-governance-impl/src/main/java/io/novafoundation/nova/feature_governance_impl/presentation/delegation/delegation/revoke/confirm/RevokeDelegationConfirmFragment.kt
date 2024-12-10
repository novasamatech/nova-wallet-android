package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm

import android.os.Bundle
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
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentRevokeDelegationConfirmBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.setVoteModelOrHide
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateLabelState
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackDelegationListBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class RevokeDelegationConfirmFragment : BaseFragment<RevokeDelegationConfirmViewModel, FragmentRevokeDelegationConfirmBinding>() {

    companion object {

        private const val PAYLOAD = "RevokeDelegationConfirmFragment.Payload"

        fun getBundle(payload: RevokeDelegationConfirmPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentRevokeDelegationConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.revokeDelegationConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.revokeDelegationConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        binder.revokeDelegationConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.revokeDelegationConfirmDelegate.setOnClickListener { viewModel.delegateClicked() }

        binder.revokeDelegationConfirmInformation.setOnAccountClickedListener { viewModel.accountClicked() }

        binder.revokeDelegationConfirmTracks.setOnClickListener { viewModel.tracksClicked() }
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
        observeHints(viewModel.hintsMixin, binder.revokeDelegationConfirmHints)

        setupFeeLoading(viewModel, binder.revokeDelegationConfirmInformation.fee)
        viewModel.currentAddressModelFlow.observe(binder.revokeDelegationConfirmInformation::setAccount)
        viewModel.walletModel.observe(binder.revokeDelegationConfirmInformation::setWallet)

        viewModel.delegateLabelModel.observe(binder.revokeDelegationConfirmDelegate::setDelegateLabelState)
        viewModel.tracksSummary.observe(binder.revokeDelegationConfirmTracks::showValue)
        viewModel.userDelegation.observe(binder.revokeDelegationConfirmDelegation::setVoteModelOrHide)

        viewModel.undelegatingPeriod.observe(binder.revokeDelegationConfirmUndelegatingPeriod::showLoadingValue)

        viewModel.showNextProgress.observe(binder.revokeDelegationConfirmConfirm::setProgressState)

        viewModel.showTracksEvent.observeEvent { tracks ->
            TrackDelegationListBottomSheet(requireContext(), tracks).show()
        }
    }
}
