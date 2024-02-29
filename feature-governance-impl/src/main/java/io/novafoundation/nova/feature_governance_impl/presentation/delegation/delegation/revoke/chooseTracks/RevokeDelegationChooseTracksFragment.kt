package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.revoke.chooseTracks.RevokeDelegationChooseTracksPayload
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.SelectDelegationTracksFragment

class RevokeDelegationChooseTracksFragment : SelectDelegationTracksFragment<RevokeDelegationChooseTracksViewModel>() {

    companion object {
        private const val EXTRA_PAYLOAD = "EXTRA_PAYLOAD"

        fun getBundle(payload: RevokeDelegationChooseTracksPayload): Bundle {
            return Bundle().apply {
                putParcelable(EXTRA_PAYLOAD, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .revokeDelegationChooseTracksFactory()
            .create(this, argument(EXTRA_PAYLOAD))
            .inject(this)
    }
}
