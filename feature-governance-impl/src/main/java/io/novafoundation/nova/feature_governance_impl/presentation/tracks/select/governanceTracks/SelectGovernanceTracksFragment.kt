package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksRequester
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.BaseSelectTracksFragment
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.adapter.SelectGovernanceTracksHeaderAdapter

class SelectGovernanceTracksFragment : BaseSelectTracksFragment<SelectGovernanceTracksViewModel>() {

    companion object {
        private const val EXTRA_PAYLOAD = "SelectGovernanceTracksFragment.Payload"

        fun getBundle(payload: SelectTracksRequester.Request): Bundle {
            return Bundle().apply {
                putParcelable(EXTRA_PAYLOAD, payload)
            }
        }
    }

    override val headerAdapter = SelectGovernanceTracksHeaderAdapter()

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        ).selectGovernanceTracks()
            .create(this, argument(EXTRA_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SelectGovernanceTracksViewModel) {
        super.subscribe(viewModel)
        viewModel.chainModel.observe { headerAdapter.setChain(it) }
    }
}
