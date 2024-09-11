package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote.ConfirmVoteFragment

class ConfirmTinderGovVoteFragment : ConfirmVoteFragment<ConfirmTinderGovVoteViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .confirmTinderGovVoteFactory()
            .create(this)
            .inject(this)
    }
}
