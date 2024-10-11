package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteDirectionModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteModel

data class YourVoteModel(
    val voteTitle: String,
    val vote: VoteModel,
    val voteDirection: VoteDirectionModel,
)

class YourVoteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_your_vote, this)
    }

    fun setVoteType(voteType: String, @ColorRes voteColorRes: Int) {
        viewYourVoteType.text = voteType
        viewYourVoteType.setTextColorRes(voteColorRes)
    }

    fun setVoteTitle(voteTitle: String) {
        viewYourVote.text = voteTitle
    }

    fun setVoteValue(value: String, valueDetails: String?) {
        viewYourVoteValue.text = value
        viewYourVoteValueDetails.text = valueDetails
    }
}

fun YourVoteView.setVoteModelOrHide(maybeModel: YourVoteModel?) = letOrHide(maybeModel) { model ->
    setVoteType(model.voteDirection.text, model.voteDirection.textColor)
    setVoteValue(model.vote.votesCount, model.vote.votesCountDetails)
    setVoteTitle(model.voteTitle)
}
