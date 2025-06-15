package io.novafoundation.nova.common.view.bottomSheet.action

import android.content.Context
import android.view.LayoutInflater
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.BottomSheetActionBinding
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet

class ActionBottomSheet(
    context: Context,
    payload: ActionBottomSheetPayload
) : BaseBottomSheet<BottomSheetActionBinding>(context, R.style.BottomSheetDialog) {

    override val binder: BottomSheetActionBinding = BottomSheetActionBinding.inflate(LayoutInflater.from(context))

    init {
        binder.setupView(payload) { dismiss() }
    }
}
