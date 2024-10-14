package io.novafoundation.nova.feature_governance_impl.presentation.track.unavailable

import android.content.Context
import android.view.LayoutInflater
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.BottomSheetUnavailableTracksBinding
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel

class UnavailableTracksPayload(
    val alreadyVoted: List<TrackModel>,
    val alreadyDelegated: List<TrackModel>
)

class UnavailableTracksBottomSheet(
    context: Context,
    private val payload: UnavailableTracksPayload,
    private val removeVotesClicked: () -> Unit
) : BaseBottomSheet<BottomSheetUnavailableTracksBinding>(context), UnavailableTracksAdapter.Handler {

    override val binder: BottomSheetUnavailableTracksBinding = BottomSheetUnavailableTracksBinding.inflate(LayoutInflater.from(context))

    private val adapter = UnavailableTracksAdapter(this)

    init {
        binder.unavailableTracksList.adapter = adapter

        adapter.submitList(buildList())
    }

    private fun buildList(): List<Any> {
        return buildList {
            if (payload.alreadyDelegated.isNotEmpty()) {
                add(UnavailableTracksGroupModel(R.string.unavailable_tracks_delegated_group, showRemoveTracksButton = false))
                addAll(payload.alreadyDelegated)
            }

            if (payload.alreadyVoted.isNotEmpty()) {
                add(UnavailableTracksGroupModel(R.string.unavailable_tracks_voted_group, showRemoveTracksButton = true))
                addAll(payload.alreadyVoted)
            }
        }
    }

    override fun removeVotesClicked() {
        removeVotesClicked.invoke()
        dismiss()
    }
}
