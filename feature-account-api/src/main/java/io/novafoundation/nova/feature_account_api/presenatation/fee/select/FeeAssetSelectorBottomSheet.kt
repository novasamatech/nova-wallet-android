package io.novafoundation.nova.feature_account_api.presenatation.fee.select

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.utils.indexOfFirstOrNull
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.BottomSheetFeeSelectionBinding
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class FeeAssetSelectorBottomSheet(
    context: Context,
    private val payload: Payload,
    val onOptionClicked: (Chain.Asset) -> Unit,
    onCancel: () -> Unit,
) : BaseBottomSheet<BottomSheetFeeSelectionBinding>(context, onCancel = onCancel) {

    override val binder: BottomSheetFeeSelectionBinding = BottomSheetFeeSelectionBinding.inflate(LayoutInflater.from(context))

    class Payload(
        val options: List<Chain.Asset>,
        val selectedOption: Chain.Asset,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        payload.options.forEach { feeOption ->
            binder.bottomSheetFeeSelectionAssets.addTab(feeOption.symbol.value)
        }

        binder.bottomSheetFeeSelectionAssets.onTabSelected { index ->
            onOptionClicked(payload.options[index])
            dismiss()
        }

        payload.selectedOptionIndex?.let { index ->
            binder.bottomSheetFeeSelectionAssets.setCheckedTab(index, triggerListener = false)
        }
    }

    private val Payload.selectedOptionIndex: Int?
        get() = options.indexOfFirstOrNull { it.id == selectedOption.id }
}
