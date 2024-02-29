package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm

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
import io.novafoundation.nova.common.view.setProgress
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.confirm.NewDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.setVoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateLabelState
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.view.setAmountChangeModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackListBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmAmount
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmConfirm
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmDelegate
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmDelegation
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmHints
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmInformation
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmLockedAmountChanges
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmLockedPeriodChanges
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmToolbar
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmTracks
import kotlinx.android.synthetic.main.fragment_new_delegation_confirm.newDelegationConfirmTransferableAmountChanges

class NewDelegationConfirmFragment : BaseFragment<NewDelegationConfirmViewModel>() {

    companion object {

        private const val PAYLOAD = "NewDelegationConfirmFragment.Payload"

        fun getBundle(payload: NewDelegationConfirmPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_new_delegation_confirm, container, false)
    }

    override fun initViews() {
        newDelegationConfirmToolbar.setHomeButtonListener { viewModel.backClicked() }

        newDelegationConfirmConfirm.prepareForProgress(viewLifecycleOwner)
        newDelegationConfirmConfirm.setOnClickListener { viewModel.confirmClicked() }

        newDelegationConfirmDelegate.setOnClickListener { viewModel.delegateClicked() }

        newDelegationConfirmInformation.setOnAccountClickedListener { viewModel.accountClicked() }

        newDelegationConfirmTracks.setOnClickListener { viewModel.tracksClicked() }
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
        observeHints(viewModel.hintsMixin, newDelegationConfirmHints)

        viewModel.title.observe(newDelegationConfirmToolbar::setTitle)

        viewModel.amountModelFlow.observe(newDelegationConfirmAmount::setAmount)

        setupFeeLoading(viewModel, newDelegationConfirmInformation.fee)
        viewModel.currentAddressModelFlow.observe(newDelegationConfirmInformation::setAccount)
        viewModel.walletModel.observe(newDelegationConfirmInformation::setWallet)

        viewModel.delegateLabelModel.observe(newDelegationConfirmDelegate::setDelegateLabelState)
        viewModel.tracksModelFlow.observe { newDelegationConfirmTracks.showValue(it.overview) }
        viewModel.delegationModel.observe(newDelegationConfirmDelegation::setVoteModel)

        viewModel.locksChangeUiFlow.observe {
            newDelegationConfirmLockedAmountChanges.setAmountChangeModel(it.amountChange)
            newDelegationConfirmLockedPeriodChanges.setAmountChangeModel(it.periodChange)
            newDelegationConfirmTransferableAmountChanges.setAmountChangeModel(it.transferableChange)
        }

        viewModel.showNextProgress.observe(newDelegationConfirmConfirm::setProgress)

        viewModel.showTracksEvent.observeEvent { tracks ->
            TrackListBottomSheet(requireContext(), tracks).show()
        }
    }
}
