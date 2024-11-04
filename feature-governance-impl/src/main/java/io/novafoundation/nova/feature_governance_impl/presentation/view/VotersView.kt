package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ViewVotersBinding

class VotersView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewVotersBinding.inflate(inflater(), this)

    init {
        View.inflate(context, R.layout.view_voters, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun setVoteType(@StringRes voteTypeRes: Int, @ColorRes voteColorRes: Int) {
        binder.votersViewVoteType.setText(voteTypeRes)
        binder.votersViewVoteTypeColor.background = getRoundedCornerDrawable(voteColorRes, cornerSizeDp = 3)
    }

    fun showLoading(loading: Boolean) {
        binder.votersViewVotesCountShimmer.isVisible = loading
        binder.votersViewVotesCount.isVisible = !loading
    }

    fun setVotesValue(value: String?) {
        binder.votersViewVotesCount.setTextOrHide(value)
    }
}

class VotersModel(
    @StringRes val voteTypeRes: Int,
    @ColorRes val voteTypeColorRes: Int,
    val votesValue: String?,
    val loading: Boolean
)

fun VotersView.setVotersModel(maybeModel: VotersModel?) = letOrHide(maybeModel) { model ->
    showLoading(model.loading)
    setVoteType(model.voteTypeRes, model.voteTypeColorRes)
    setVotesValue(model.votesValue)
}
