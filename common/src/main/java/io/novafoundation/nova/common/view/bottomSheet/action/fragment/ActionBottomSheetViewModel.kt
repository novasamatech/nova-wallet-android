package io.novafoundation.nova.common.view.bottomSheet.action.fragment

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetPayload

abstract class ActionBottomSheetViewModel(
    private val returnableRouter: ReturnableRouter
) : BaseViewModel() {

    abstract fun getPayload(): ActionBottomSheetPayload

    fun back() {
        returnableRouter.back()
    }
}
