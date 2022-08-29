package io.novafoundation.nova.feature_account_api.presenatation.account.createName

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

abstract class CreateWalletNameViewModel(
    private val router: ReturnableRouter,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val name = MutableStateFlow("")

    val continueState = name.map {
        if (it.isNotEmpty()) {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        } else {
            DescriptiveButtonState.Disabled(resourceManager.getString(R.string.account_enter_wallet_nickname))
        }
    }

    abstract fun proceed(name: String)

    fun homeButtonClicked() {
        router.back()
    }

    fun nextClicked() {
        proceed(name.value)
    }
}
