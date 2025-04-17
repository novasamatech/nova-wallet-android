package io.novafoundation.nova.feature_assets.presentation.balance.detail

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class LockedTokensBottomSheet(
    context: Context,
    private val balanceLocks: BalanceLocksModel
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context)) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_balance_locked)
        val viewItems = createViewItems(balanceLocks.locks)
        viewItems.forEach { addItem(it) }
    }

    private fun createViewItems(locks: List<BalanceLocksModel.Lock>): List<TableCellView> {
        return locks.map(::createViewItem)
    }

    private fun createViewItem(lock: BalanceLocksModel.Lock): TableCellView {
        return TableCellView.createTableCellView(context).apply {
            setOwnDividerVisible(false)
            setTitle(lock.name)
            showAmount(lock.amount)
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    left = getCommonPadding(),
                    right = getCommonPadding()
                )
            }
        }
    }
}
