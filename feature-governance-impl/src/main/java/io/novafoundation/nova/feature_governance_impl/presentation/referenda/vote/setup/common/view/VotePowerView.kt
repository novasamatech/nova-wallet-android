package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.input.seekbar.Seekbar
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_vote_power.view.votePowerSelector
import kotlinx.android.synthetic.main.view_vote_power.view.votePowerVotes

class VotePowerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    val votePowerSeekbar: Seekbar
        get() = votePowerSelector

    val votePowerVotesText: TextView
        get() = votePowerVotes

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_vote_power, this)

        setPadding(16.dp)

        background = getRoundedCornerDrawable(fillColorRes = R.color.block_background)
    }
}
