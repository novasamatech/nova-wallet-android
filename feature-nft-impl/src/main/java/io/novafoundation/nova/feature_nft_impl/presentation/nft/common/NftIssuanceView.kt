package io.novafoundation.nova.feature_nft_impl.presentation.nft.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_nft_impl.R

class NftIssuanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        setTextAppearance(R.style.TextAppearance_NovaFoundation_SemiBold_Caps2)
        setTextColorRes(R.color.white_64)
        updatePadding(top = 1.5f.dp, bottom = 1.5f.dp, start = 6.dp, end = 8.dp)
        background = context.getRoundedCornerDrawable(R.color.white_16, cornerSizeInDp = 4)
    }
}
