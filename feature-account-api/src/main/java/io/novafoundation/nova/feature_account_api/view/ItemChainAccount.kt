package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ItemChainAccountBinding

class ItemChainAccount @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binder = ItemChainAccountBinding.inflate(inflater(), this)

    val chainIcon: ImageView
        get() = binder.chainAccountChainIcon

    val chainName: TextView
        get() = binder.chainAccountChainName

    val accountIcon: ImageView
        get() = binder.chainAccountAccountIcon

    val accountAddress: TextView
        get() = binder.chainAccountAccountAddress

    val action: ImageView
        get() = binder.labeledTextAction

    init {
        View.inflate(context, R.layout.item_chain_account, this)

        background = context.getDrawableCompat(R.drawable.bg_primary_list_item)
    }
}
