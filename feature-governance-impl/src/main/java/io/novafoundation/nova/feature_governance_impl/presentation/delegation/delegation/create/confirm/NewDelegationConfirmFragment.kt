package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm

import android.os.Bundle
import androidx.core.os.bundleOf
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentNewDelegationConfirmBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.setVoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateLabelState
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackListBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading

class NewDelegationConfirmFragment : BaseFragment<NewDelegationConfirmViewModel, FragmentNewDelegationConfirmBinding>() {

    companion object {

        private const val PAYLOAD = "NewDelegationConfirmFragment.Payload"

        fun getBundle(payload: NewDelegationConfirmPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun createBinding() = FragmentNewDelegationConfirmBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.newDelegationConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.newDelegationConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        binder.newDelegationConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        binder.newDelegationConfirmDelegate.setOnClickListener { viewModel.delegateClicked() }

        binder.newDelegationConfirmInformation.setOnAccountClickedListener { viewModel.accountClicked() }

        binder.newDelegationConfirmTracks.setOnClickListener { viewModel.tracksClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .newDelegationConfirmFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: NewDelegationConfirmViewModel) {
        observeRetries(viewModel.partialRetriableMixin)
        observeValidations(viewModel)
        setupExternalActions(viewModel)
        observeHints(viewModel.hintsMixin, binder.newDelegationConfirmHints)

        viewModel.title.observe(binder.newDelegationConfirmToolbar::setTitle)

        viewModel.amountModelFlow.observe(binder.newDelegationConfirmAmount::setAmount)

        setupFeeLoading(viewModel, binder.newDelegationConfirmInformation.fee)
        viewModel.currentAddressModelFlow.observe(binder.newDelegationConfirmInformation::setAccount)
        viewModel.walletModel.observe(binder.newDelegationConfirmInformation::setWallet)

        viewModel.delegateLabelModel.observe(binder.newDelegationConfirmDelegate::setDelegateLabelState)
        viewModel.tracksModelFlow.observe { binder.newDelegationConfirmTracks.showValue(it.overview) }
        viewModel.delegationModel.observe(binder.newDelegationConfirmDelegation::setVoteModel)

        viewModel.locksChangeUiFlow.observe {
            binder.newDelegationConfirmLockedAmountChanges.setAmountChangeModel(it.amountChange)
            binder.newDelegationConfirmLockedPeriodChanges.setAmountChangeModel(it.periodChange)
            binder.newDelegationConfirmTransferableAmountChanges.setAmountChangeModel(it.transferableChange)
        }

        viewModel.showNextProgress.observe(binder.newDelegationConfirmConfirm::setProgressState)

        viewModel.showTracksEvent.observeEvent { tracks ->
            TrackListBottomSheet(requireContext(), tracks).show()
        }
    }
}
