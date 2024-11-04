package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.input.seekbar.Seekbar
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ViewVotePowerBinding

class VotePowerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewVotePowerBinding.inflate(inflater(), this)

    val votePowerSeekbar: Seekbar
        get() = binder.votePowerSelector

    val votePowerVotesText: TextView
        get() = binder.votePowerVotes

    init {
        orientation = VERTICAL

        setPadding(16.dp)

        background = getRoundedCornerDrawable(fillColorRes = R.color.block_background)
    }
}
