package io.novafoundation.nova.feature_assets.presentation.send

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_assets.presentation.common.currencyItem
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import java.math.BigDecimal

class BalanceDetailsBottomSheet(
    context: Context,
    private val payload: Payload,
) : FixedListBottomSheet(context) {

    class Payload(
        val assetModel: AssetModel,
        val transferDraft: TransferDraft,
        val existentialDeposit: BigDecimal
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.wallet_balance_details_title)

        with(payload) {
            currencyItem(R.string.choose_amount_available_balance, assetModel.available)
            currencyItem(R.string.wallet_balance_details_total, assetModel.total)
            currencyItem(R.string.wallet_balance_details_total_after, transferDraft.totalAfterTransfer(assetModel.total))
            currencyItem(R.string.wallet_send_balance_minimal, existentialDeposit)
        }
    }
}
