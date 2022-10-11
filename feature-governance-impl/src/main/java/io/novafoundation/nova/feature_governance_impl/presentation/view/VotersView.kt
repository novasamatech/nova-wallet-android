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
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_voters, this)
        background = getRoundedCornerDrawable(R.color.transparent).withRipple()
        votersViewVotesCount.setDrawableEnd(R.drawable.ic_info_16, widthInDp = 16, tint = R.color.white_48, paddingInDp = 8)
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
}
