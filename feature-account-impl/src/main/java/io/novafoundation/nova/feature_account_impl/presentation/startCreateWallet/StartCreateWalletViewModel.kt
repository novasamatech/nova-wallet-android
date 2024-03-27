package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

enum class CreateWalletState {
    SETUP_NAME,
    CHOOSE_BACKUP_WAY
}

class StartCreateWalletViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val nameInput = MutableStateFlow("")

    private val _createWalletState = MutableStateFlow(CreateWalletState.SETUP_NAME)
    val createWalletState: Flow<CreateWalletState> = _createWalletState

    val confirmNameButtonState: Flow<DescriptiveButtonState> = nameInput.map { name ->
        if (name.isEmpty()) {
            DescriptiveButtonState.Disabled(resourceManager.getString(R.string.start_create_wallet_enter_wallet_name))
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_confirm))
        }
    }.shareInBackground()

    val explanationText: Flow<String> = createWalletState.map {
        when (it) {
            CreateWalletState.SETUP_NAME -> resourceManager.getString(R.string.account_create_name_subtitle_v2_2_0)
            CreateWalletState.CHOOSE_BACKUP_WAY -> resourceManager.getString(R.string.start_create_wallet_backup_ready_explanation)
        }
    }.shareInBackground()

    fun backClicked() {
        if (_createWalletState.value == CreateWalletState.SETUP_NAME) {
            router.back()
        } else {
            _createWalletState.value = CreateWalletState.SETUP_NAME
        }
    }

    fun confirmNameClicked() {
        _createWalletState.value = CreateWalletState.CHOOSE_BACKUP_WAY
    }

    fun cloudBackupClicked() {

    }

    fun manualBackupClicked() {
        router.openMnemonicScreen(null, AddAccountPayload.MetaAccount)
    }
}
