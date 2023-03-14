package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getBlockDrawable

class YourMultiVoteModel(val votes: List<YourVoteModel>)

class YourMultiVoteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private var currentModel: YourMultiVoteModel? = null

    init {
        orientation = VERTICAL

        setPadding(0, 5.dp, 0, 5.dp)

        background = context.getBlockDrawable()
    }

    fun setModel(model: YourMultiVoteModel?) {
        if (model != null && model == currentModel) {
            return
        }
        currentModel = model

        val votes = model?.votes.orEmpty()

        setVisible(votes.isNotEmpty())

        removeAllViews()

        votes.forEach { voteModel ->
            val voteView = YourVoteView(context)
            voteView.setVoteModelOrHide(voteModel)
            addView(voteView)
        }
    }
}
