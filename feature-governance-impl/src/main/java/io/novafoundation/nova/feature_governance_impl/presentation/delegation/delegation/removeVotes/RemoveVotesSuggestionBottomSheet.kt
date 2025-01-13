package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.BottomRemoveVotesSuggestionBinding

class RemoveVotesSuggestionBottomSheet(
    context: Context,
    private val votesCount: Int,
    private val onApply: () -> Unit,
    private val onSkip: () -> Unit,
) : BaseBottomSheet<BottomRemoveVotesSuggestionBinding>(context) {

    override val binder: BottomRemoveVotesSuggestionBinding = BottomRemoveVotesSuggestionBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder.removeVotesSuggestionDescription.text = context.resources.getQuantityString(
            R.plurals.remove_votes_suggestion_description,
            votesCount,
            votesCount
        )
        binder.removeVotesSuggestionSkip.setDismissingClickListener {
            onSkip()
        }
        binder.removeVotesSuggestionApply.setDismissingClickListener {
            onApply()
        }
    }
}
