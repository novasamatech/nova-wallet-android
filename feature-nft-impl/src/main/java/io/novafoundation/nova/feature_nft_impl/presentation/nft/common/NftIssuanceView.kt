package io.novafoundation.nova.feature_nft_impl.presentation.nft.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.R
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

class NftIssuanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(ContextThemeWrapper(context, R.style.Widget_Nova_NftIssuance), attrs, defStyleAttr) {

    init {
        background = context.getRoundedCornerDrawable(R.color.white_16, cornerSizeInDp = 4)
    }
}
