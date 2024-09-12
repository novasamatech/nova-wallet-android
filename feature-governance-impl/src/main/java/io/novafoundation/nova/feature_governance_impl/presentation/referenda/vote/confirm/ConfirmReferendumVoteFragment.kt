package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm

import android.os.Bundle
import androidx.core.os.bundleOf
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote.ConfirmVoteFragment

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
