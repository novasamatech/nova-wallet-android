package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.GridSpacingItemDecoration
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_account_impl.R
import kotlin.math.roundToInt

private const val DEFAULT_COLUMNS = 3

class MnemonicContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val _layoutManager = GridLayoutManager(context, DEFAULT_COLUMNS)
    private var itemDecoration: ItemDecoration? = null

    init {
        layoutManager = _layoutManager

        attrs?.let(::applyAttrs)
    }

    fun setItemPadding(padding: Int) {
        itemDecoration?.let { removeItemDecoration(it) }
        itemDecoration = GridSpacingItemDecoration(_layoutManager, padding)
        itemDecoration?.let { addItemDecoration(it) }
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.MnemonicContainerView) {
        if (it.hasValue(R.styleable.MnemonicContainerView_paddingBetweenItems)) {
            val padding = it.getDimension(R.styleable.MnemonicContainerView_paddingBetweenItems, 0f).roundToInt()
            setItemPadding(padding)
        }
    }
}
