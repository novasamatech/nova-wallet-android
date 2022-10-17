package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.useNonNullOrHide
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_voters.view.votersViewVoteType
import kotlinx.android.synthetic.main.view_voters.view.votersViewVoteTypeColor
import kotlinx.android.synthetic.main.view_voters.view.votersViewVotesCount

class VotersView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_voters, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun setVoteType(@StringRes voteTypeRes: Int, @ColorRes voteColorRes: Int) {
        votersViewVoteType.setText(voteTypeRes)
        votersViewVoteTypeColor.background = getRoundedCornerDrawable(voteColorRes, cornerSizeDp = 3)
    }

    fun setVotesValue(value: String) {
        votersViewVotesCount.text = value
    }
}

class VotersModel(
    @StringRes val voteTypeRes: Int,
    @ColorRes val voteTypeColorRes: Int,
    val votesValue: String
)

fun VotersView.setVotersModel(maybeModel: VotersModel?) = useNonNullOrHide(maybeModel) { model ->
    setVoteType(model.voteTypeRes, model.voteTypeColorRes)
    setVotesValue(model.votesValue)
}
