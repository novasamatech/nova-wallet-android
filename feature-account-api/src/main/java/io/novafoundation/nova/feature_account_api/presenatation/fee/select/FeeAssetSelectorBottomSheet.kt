package io.novafoundation.nova.feature_account_api.presenatation.fee.select

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.utils.indexOfFirstOrNull
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.android.synthetic.main.bottom_sheet_fee_selection.bottomSheetFeeSelectionAssets

class FeeAssetSelectorBottomSheet(
    context: Context,
    private val payload: Payload,
    val onOptionClicked: (Chain.Asset) -> Unit,
    onCancel: () -> Unit,
) : BaseBottomSheet(context, onCancel = onCancel) {

    class Payload(
        val options: List<Chain.Asset>,
        val selectedOption: Chain.Asset,
    )

    init {
        setContentView(R.layout.bottom_sheet_fee_selection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        payload.options.forEach { feeOption ->
            bottomSheetFeeSelectionAssets.addTab(feeOption.symbol.value)
        }

        bottomSheetFeeSelectionAssets.onTabSelected { index ->
            onOptionClicked(payload.options[index])
            dismiss()
        }

        payload.selectedOptionIndex?.let { index ->
            bottomSheetFeeSelectionAssets.setCheckedTab(index, triggerListener = false)
        }
    }

    private val Payload.selectedOptionIndex: Int?
        get() = options.indexOfFirstOrNull { it.id == selectedOption.id }
}
