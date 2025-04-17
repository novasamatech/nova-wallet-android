package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_governance_impl.databinding.ViewYourVotePreviewBinding
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourMultiVotePreviewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVotePreviewModel

import kotlin.math.max

private const val MAX_SUPPORTED_VOTE_TYPES = 3

class YourMultiVotePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private val voteTypeViews = Array<YourVotePreviewView?>(MAX_SUPPORTED_VOTE_TYPES) { null }

    init {
        orientation = VERTICAL
    }

    fun setModel(model: YourMultiVotePreviewModel?) {
        val votes = model?.votes.orEmpty()

        val itemsToModify = max(voteTypeViews.size, votes.size)

        for (index in 0 until itemsToModify) {
            if (index < votes.size) { // we want to show content
                val view = getOrCreateView(index) ?: return

                view.makeVisible()
                view.setModel(votes[index])
            } else { // we want to hide view if present
                getViewOrNull(index)?.makeGone()
            }
        }
    }

    private fun getOrCreateView(index: Int): YourVotePreviewView? {
        if (index >= MAX_SUPPORTED_VOTE_TYPES) return null

        return voteTypeViews.getOrNull(index) ?: createNewView().also {
            voteTypeViews[index] = it
            addView(it)
        }
    }

    private fun getViewOrNull(index: Int): YourVotePreviewView? {
        return voteTypeViews.getOrNull(index)
    }

    private fun createNewView(): YourVotePreviewView {
        return YourVotePreviewView(context)
    }
}

class YourVotePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewYourVotePreviewBinding.inflate(inflater(), this)

    init {
        setPadding(0, 12.dp, 0, 0)
    }

    fun setModel(maybeModel: YourVotePreviewModel?) = letOrHide(maybeModel) { model ->
        binder.itemReferendumYourVoteType.text = model.voteDirection.text
        binder.itemReferendumYourVoteType.setTextColorRes(model.voteDirection.textColor)
        binder.itemReferendumYourVoteDetails.text = model.details
    }
}
