package io.novafoundation.nova.feature_governance_impl.presentation.track.list

import android.content.Context
import android.os.Bundle
import android.view.View
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ReferentialEqualityDiffCallBack
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackDelegationModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.setTrackModel

class TrackDelegationListBottomSheet(
    context: Context,
    data: List<TrackDelegationModel>,
) : DynamicListBottomSheet<TrackDelegationModel>(context, Payload(data), ReferentialEqualityDiffCallBack(), onClicked = null) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.delegation_tracks)
    }

    override fun holderCreator(): HolderCreator<TrackDelegationModel> = {
        TrackDelegationHolder(it.inflateChild(R.layout.item_track_delegation))
    }
}

class TrackDelegationHolder(
    itemView: View
) : DynamicListSheetAdapter.Holder<TrackDelegationModel>(itemView) {

    override fun bind(
        item: TrackDelegationModel,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<TrackDelegationModel>
    ) = with(itemView) {
        itemTrackDelegationTrack.setTrackModel(item.track)
        itemTrackDelegationVotesCount.text = item.delegation.votesCount
        itemTrackDelegationVotesCountDetails.text = item.delegation.votesCountDetails
    }
}
