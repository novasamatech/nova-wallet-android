package io.novafoundation.nova.feature_wallet_impl.presentation.common

import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_wallet_impl.R
import kotlinx.android.synthetic.main.item_sheet_currency.view.itemCurrencyLabel
import kotlinx.android.synthetic.main.item_sheet_currency.view.itemCurrencyValue
import java.math.BigDecimal

fun FixedListBottomSheet.currencyItem(@StringRes label: Int, value: BigDecimal) {
    item(R.layout.item_sheet_currency) { view ->
        view.itemCurrencyLabel.setText(label)

        view.itemCurrencyValue.text = value.format()
    }
}
