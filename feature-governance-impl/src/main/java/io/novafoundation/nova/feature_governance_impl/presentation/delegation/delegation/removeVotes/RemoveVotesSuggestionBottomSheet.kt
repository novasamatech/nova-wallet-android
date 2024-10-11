package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_governance_impl.R

class RemoveVotesSuggestionBottomSheet(
    context: Context,
    private val votesCount: Int,
    private val onApply: () -> Unit,
    private val onSkip: () -> Unit,
) : BaseBottomSheet(context) {

    init {
        setContentView(R.layout.bottom_remove_votes_suggestion)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        removeVotesSuggestionDescription.text = context.resources.getQuantityString(
            R.plurals.remove_votes_suggestion_description,
            votesCount,
            votesCount
        )
        removeVotesSuggestionSkip.setDismissingClickListener {
            onSkip()
        }
        removeVotesSuggestionApply.setDismissingClickListener {
            onApply()
        }
    }
}
