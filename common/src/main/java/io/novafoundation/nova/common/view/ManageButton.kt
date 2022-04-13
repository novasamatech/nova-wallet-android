package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.updatePadding

class ManageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        updatePadding(top = 6.dp, bottom = 6.dp, start = 12.dp, end = 12.dp)

        setImageResource(R.drawable.ic_options)
        setImageTintRes(R.color.white_64)

        background = addRipple(getRoundedCornerDrawable(R.color.black_48))
    }
}
