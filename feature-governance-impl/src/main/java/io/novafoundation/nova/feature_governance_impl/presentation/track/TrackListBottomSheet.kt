package io.novafoundation.nova.feature_governance_impl.presentation.track

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter.Handler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.item_track.view.itemTrack

class TrackListBottomSheet(
    context: Context,
    data: List<TrackModel>,
) : DynamicListBottomSheet<TrackModel>(context, Payload(data), AccountDiffCallback, onClicked = null) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.delegation_tracks)
    }

    override fun holderCreator(): HolderCreator<TrackModel> = {
        AccountHolder(it.inflateChild(R.layout.item_track))
    }
}

class AccountHolder(
    itemView: View
) : DynamicListSheetAdapter.Holder<TrackModel>(itemView) {

    override fun bind(item: TrackModel, isSelected: Boolean, handler: Handler<TrackModel>) {
        itemView.itemTrack.setTrackModel(item)
    }
}

private object AccountDiffCallback : DiffUtil.ItemCallback<TrackModel>() {
    override fun areItemsTheSame(oldItem: TrackModel, newItem: TrackModel): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: TrackModel, newItem: TrackModel): Boolean {
        return true
    }
}
