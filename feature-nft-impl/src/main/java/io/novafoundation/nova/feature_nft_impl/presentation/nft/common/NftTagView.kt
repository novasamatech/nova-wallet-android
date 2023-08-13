package io.novafoundation.nova.feature_nft_impl.presentation.nft.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_nft_impl.R

class NftTagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        setTextAppearance(R.style.TextAppearance_NovaFoundation_SemiBold_Caps2)
        setTextColorRes(R.color.chip_text)
        updatePadding(top = 1.5f.dp, bottom = 1.5f.dp, start = 6.dp, end = 6.dp)
        isAllCaps = false
        background = context.getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 4)
    }
}
