package io.novafoundation.nova.common.view.bottomSheet.action.fragment

import android.view.LayoutInflater
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.databinding.BottomSheetActionBinding
import io.novafoundation.nova.common.view.bottomSheet.action.setupView

abstract class ActionBottomSheetDialogFragment<T : ActionBottomSheetViewModel> : BaseBottomSheetFragment<T, BottomSheetActionBinding>() {

    override fun createBinding() = BottomSheetActionBinding.inflate(LayoutInflater.from(context))

    override fun initViews() {
        binder.setupView(viewModel.getPayload()) { tryToDismiss() }
    }

    open fun tryToDismiss() {
        viewModel.back()
    }
}
