package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.SelectDelegationTracksFragment

class NewDelegationChooseTracksFragment : SelectDelegationTracksFragment<NewDelegationChooseTracksViewModel>() {

    companion object {
        private const val EXTRA_PAYLOAD = "EXTRA_PAYLOAD"

        fun getBundle(payload: NewDelegationChooseTracksPayload): Bundle {
            return Bundle().apply {
                putParcelable(EXTRA_PAYLOAD, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        ).newDelegationChooseTracks()
            .create(this, argument(EXTRA_PAYLOAD))
            .inject(this)
    }
}
