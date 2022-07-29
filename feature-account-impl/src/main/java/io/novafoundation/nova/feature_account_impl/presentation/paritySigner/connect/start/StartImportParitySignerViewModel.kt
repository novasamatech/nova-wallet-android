package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class StartImportParitySignerViewModel(
    private val router: AccountRouter,
) : BaseViewModel() {

    fun backClicked() {
        router.back()
    }

    fun scanQrCodeClicked() {
        showMessage("TODO")
    }
}
