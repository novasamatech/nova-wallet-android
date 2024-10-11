package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.feature_account_api.R

class ItemChainAccount @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val chainIcon: ImageView
        get() = chainAccountChainIcon

    val chainName: TextView
        get() = chainAccountChainName

    val accountIcon: ImageView
        get() = chainAccountAccountIcon

    val accountAddress: TextView
        get() = chainAccountAccountAddress

    val action: ImageView
        get() = labeledTextAction

    init {
        View.inflate(context, R.layout.item_chain_account, this)

        background = context.getDrawableCompat(R.drawable.bg_primary_list_item)
    }
}
