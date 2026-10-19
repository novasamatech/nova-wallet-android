package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_staking_impl.databinding.BottomSheetBittensorValidatorFiltersBinding
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting

data class BittensorValidatorFilters(
    val sorting: BittensorValidatorSorting = BittensorValidatorSorting.APY,
    val hideUnusualApy: Boolean = true
)

class BittensorValidatorFiltersBottomSheet(
    context: Context,
    private val initial: BittensorValidatorFilters,
    private val onApply: (BittensorValidatorFilters) -> Unit
) : BaseBottomSheet<BottomSheetBittensorValidatorFiltersBinding>(context) {

    override val binder = BottomSheetBittensorValidatorFiltersBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder.hideUnusual.field.isChecked = initial.hideUnusualApy
        val selected = when (initial.sorting) {
            BittensorValidatorSorting.APY -> binder.apy
            BittensorValidatorSorting.TOTAL_STAKE -> binder.stake
            BittensorValidatorSorting.NAME -> binder.name
        }
        selected.isChecked = true
        binder.apply.setOnClickListener {
            val sorting = when (binder.sorting.checkedRadioButtonId) {
                binder.stake.id -> BittensorValidatorSorting.TOTAL_STAKE
                binder.name.id -> BittensorValidatorSorting.NAME
                else -> BittensorValidatorSorting.APY
            }
            onApply(BittensorValidatorFilters(sorting, binder.hideUnusual.field.isChecked))
            dismiss()
        }
    }
}
