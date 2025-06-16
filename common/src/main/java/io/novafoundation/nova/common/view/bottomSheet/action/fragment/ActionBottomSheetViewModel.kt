package io.novafoundation.nova.common.view.bottomSheet.action.fragment

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetPayload

abstract class ActionBottomSheetViewModel() : BaseViewModel() {

    abstract fun getPayload(): ActionBottomSheetPayload

    abstract fun onActionClicked()
}
