package io.novafoundation.nova.feature_governance_impl.presentation.track.list

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ReferentialEqualityDiffCallBack
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ItemTrackDelegationBinding
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
        TrackDelegationHolder(ItemTrackDelegationBinding.inflate(it.inflater(), it, false))
    }
}

class TrackDelegationHolder(
    private val binder: ItemTrackDelegationBinding
) : DynamicListSheetAdapter.Holder<TrackDelegationModel>(binder.root) {

    override fun bind(
        item: TrackDelegationModel,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<TrackDelegationModel>
    ) = with(binder) {
        itemTrackDelegationTrack.setTrackModel(item.track)
        itemTrackDelegationVotesCount.text = item.delegation.votesCount
        itemTrackDelegationVotesCountDetails.text = item.delegation.votesCountDetails
    }
}
