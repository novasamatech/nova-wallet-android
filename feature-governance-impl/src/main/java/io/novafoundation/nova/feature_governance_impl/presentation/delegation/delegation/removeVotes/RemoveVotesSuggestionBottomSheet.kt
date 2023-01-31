package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.bottom_remove_votes_suggestion.removeVotesSuggestionApply
import kotlinx.android.synthetic.main.bottom_remove_votes_suggestion.removeVotesSuggestionSkip

class RemoveVotesSuggestionBottomSheet(
    context: Context,
    private val onApply: () -> Unit,
) : BaseBottomSheet(context) {

    val skip: PrimaryButton
        get() = removeVotesSuggestionSkip

    val apply: PrimaryButton
        get() = removeVotesSuggestionApply

    init {
        setContentView(R.layout.bottom_remove_votes_suggestion)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        skip.setOnClickListener { dismiss() }
        apply.setOnClickListener {
            onApply()
            dismiss()
        }
    }
}
