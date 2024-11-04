package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ViewYourDelegationBinding
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoteModel

class YourDelegationModel(
    val trackSummary: String,
    val vote: VoteModel?
)

class YourDelegationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewYourDelegationBinding.inflate(inflater(), this)

    init {
        background = getRoundedCornerDrawable(R.color.block_background)

        orientation = VERTICAL
    }

    fun onTracksClicked(listener: () -> Unit) {
        binder.viewYourDelegationTracks.setOnClickListener { listener() }
    }

    fun onRevokeClicked(listener: () -> Unit) {
        binder.viewYourDelegationRemove.setOnClickListener { listener() }
    }

    fun onEditClicked(listener: () -> Unit) {
        binder.viewYourDelegationEdit.setOnClickListener { listener() }
    }

    fun setTrackSummary(summary: String) {
        binder.viewYourDelegationTracks.showValue(summary)
    }

    fun setVote(vote: VoteModel?) {
        binder.viewYourDelegationDelegation.showValueOrHide(vote?.votesCount, vote?.votesCountDetails)
    }
}

fun YourDelegationView.setModel(maybeModel: YourDelegationModel?) = letOrHide(maybeModel) { model ->
    setTrackSummary(model.trackSummary)
    setVote(model.vote)
}
