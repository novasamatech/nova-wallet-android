package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_staking_impl.databinding.BottomSheetBittensorSubnetFiltersBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BittensorSubnetFiltersBottomSheet(
    context: Context,
    initialHideThinPools: Boolean,
    private val resultLabel: (Boolean) -> Flow<String>,
    private val description: String,
    private val onApply: (Boolean) -> Unit
) : BaseBottomSheet<BottomSheetBittensorSubnetFiltersBinding>(context) {

    override val binder = BottomSheetBittensorSubnetFiltersBinding.inflate(LayoutInflater.from(context))

    private val hideThinPools = MutableStateFlow(initialHideThinPools)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder.hideThinPools.setSubtitle(description)
        binder.hideThinPools.field.isChecked = hideThinPools.value
        binder.hideThinPools.field.setOnCheckedChangeListener { _, checked -> hideThinPools.value = checked }
        binder.apply.setOnClickListener {
            onApply(hideThinPools.value)
            dismiss()
        }
        hideThinPools.flatMapLatest(resultLabel)
            .onEach { binder.apply.text = it }
            .launchIn(this)
    }
}
