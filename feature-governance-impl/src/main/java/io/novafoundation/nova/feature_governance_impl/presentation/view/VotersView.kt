package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_voters.view.votersViewCount
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
        votersViewVotesCount.setDrawableEnd(R.drawable.ic_info_16, widthInDp = 16, tint = R.color.white_48, paddingInDp = 8)
    }

    fun setVotersModel(model: VotersModel) {
        setVoteType(model.voteTypeRes, model.voteTypeColorRes)
        setVotersValue(model.votersValue)
        setVotesValue(model.votesValue)
    }

    fun setVoteType(@StringRes voteTypeRes: Int, @ColorRes voteColorRes: Int) {
        votersViewVoteType.setText(voteTypeRes)
        votersViewVoteTypeColor.background = getRoundedCornerDrawable(voteColorRes, cornerSizeDp = 3)
    }

    fun setVotersValue(value: String) {
        votersViewCount.text = value
    }

    fun setVotesValue(value: String) {
        votersViewVotesCount.text = value
    }

    class VotersModel(
        @StringRes val voteTypeRes: Int,
        @ColorRes val voteTypeColorRes: Int,
        val votersValue: String,
        val votesValue: String
    )
}
