package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class LockedTokensBottomSheet(
    context: Context,
    private val balanceLocks: BalanceLocksModel
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_balance_locked_template)
        val viewItems = createViewItems(balanceLocks.locks)
        viewItems.forEach { addItem(it) }
    }

    private fun createViewItems(locks: List<BalanceLocksModel.Lock>): List<TableCellView> {
        return TableCellView.buildFixedList(locks) {
            val view = TableCellView.createTableCellView(context)
            view.setTitle(it.formattedId(context))
            view.setDividerColor(R.color.white_8)
            view.showAmount(it.amount)
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    left = getCommonPadding(),
                    right = getCommonPadding()
                )
            }
            view
        }
    }
}
