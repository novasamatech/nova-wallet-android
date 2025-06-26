package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_governance_impl.databinding.ViewYourVoteBinding
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

    private val binder = ViewYourVoteBinding.inflate(inflater(), this)

    fun setVoteType(voteType: String, @ColorRes voteColorRes: Int) {
        binder.viewYourVoteType.text = voteType
        binder.viewYourVoteType.setTextColorRes(voteColorRes)
    }

    fun setVoteTitle(voteTitle: String) {
        binder.viewYourVote.text = voteTitle
    }

    fun setVoteValue(value: String, valueDetails: String?) {
        binder.viewYourVoteValue.text = value
        binder.viewYourVoteValueDetails.text = valueDetails
    }
}

fun YourVoteView.setVoteModelOrHide(maybeModel: YourVoteModel?) = letOrHide(maybeModel) { model ->
    setVoteType(model.voteDirection.text, model.voteDirection.textColor)
    setVoteValue(model.vote.votesCount, model.vote.votesCountDetails)
    setVoteTitle(model.voteTitle)
}
