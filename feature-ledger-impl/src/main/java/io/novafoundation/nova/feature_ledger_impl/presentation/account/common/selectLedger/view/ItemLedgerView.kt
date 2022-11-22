package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_ledger_impl.R

class ItemLedgerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_SubHeadline)
        setTextColorRes(R.color.text_primary)

        setDrawableEnd(R.drawable.ic_chevron_right, widthInDp = 24, paddingInDp = 4, tint = R.color.icon_secondary)

        updatePadding(
            top = 14.dp,
            bottom = 14.dp,
            start = 12.dp,
            end = 12.dp
        )
    }
}
