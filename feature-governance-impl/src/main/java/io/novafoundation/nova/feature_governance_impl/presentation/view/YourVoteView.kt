package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.useNonNullOrHide
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_your_vote.view.viewYourVoteType
import kotlinx.android.synthetic.main.view_your_vote.view.viewYourVoteValue
import kotlinx.android.synthetic.main.view_your_vote.view.viewYourVoteValueDetails

class YourVoteModel(
    @StringRes val voteTypeTitleRes: Int,
    @ColorRes val voteTypeColorRes: Int,
    val votes: String,
    val votesDetails: String
)

class YourVoteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_your_vote, this)
        background = getRoundedCornerDrawable(R.color.white_8)
    }

    fun setVoteType(@StringRes voteTypeRes: Int, @ColorRes voteColorRes: Int) {
        viewYourVoteType.setText(voteTypeRes)
        viewYourVoteType.setTextColorRes(voteColorRes)
    }

    fun setVoteValue(value: String, valueDetails: String) {
        viewYourVoteValue.text = value
        viewYourVoteValueDetails.text = valueDetails
    }
}

fun YourVoteView.setVoteModelOrHide(maybeModel: YourVoteModel?) = useNonNullOrHide(maybeModel) { model ->
    setVoteType(model.voteTypeTitleRes, model.voteTypeColorRes)
    setVoteValue(model.votes, model.votesDetails)
}
