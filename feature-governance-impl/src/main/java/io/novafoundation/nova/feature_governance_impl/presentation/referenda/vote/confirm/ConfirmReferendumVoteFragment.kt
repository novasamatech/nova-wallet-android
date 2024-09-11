package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.hints.observeHints
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote.ConfirmVoteFragment
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view.setAmountChangeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteAmount
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteConfirm
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteHints
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteInformation
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteLockedAmountChanges
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteLockedPeriodChanges
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteResult
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteToolbar
import kotlinx.android.synthetic.main.fragment_referendum_confirm_vote.confirmReferendumVoteTransferableAmountChanges

class ConfirmReferendumVoteFragment : ConfirmVoteFragment<ConfirmReferendumVoteViewModel>() {

    companion object {

        private const val PAYLOAD = "ConfirmReferendumVoteFragment.Payload"

        fun getBundle(payload: ConfirmVoteReferendumPayload): Bundle = bundleOf(PAYLOAD to payload)
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .confirmReferendumVoteFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }
}
