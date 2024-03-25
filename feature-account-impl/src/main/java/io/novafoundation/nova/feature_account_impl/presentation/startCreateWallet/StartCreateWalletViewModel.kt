package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class StartCreateWalletViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val nameInput = MutableStateFlow("")

    val confirmNameButtonState: Flow<DescriptiveButtonState> = nameInput.map { name ->
        if (name.isEmpty()) {
            DescriptiveButtonState.Disabled(resourceManager.getString(R.string.start_create_wallet_enter_wallet_name))
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_confirm))
        }
    }

    fun backClicked() {
        router.back()
    }
}
