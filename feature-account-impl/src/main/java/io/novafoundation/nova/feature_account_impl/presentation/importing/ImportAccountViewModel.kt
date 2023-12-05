package io.novafoundation.nova.feature_account_impl.presentation.importing

import android.content.Intent
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withFlagSet
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountAlreadyExistsException
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapAddAccountPayloadToAddAccountType
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithAccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.ImportSourceFactory
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.FileRequester
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.ImportError
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
import jp.co.soramitsu.fearless_utils.encrypt.junction.JunctionDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ImportAccountViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
    accountNameChooserFactory: MixinFactory<AccountNameChooserMixin.Presentation>,
    private val payload: ImportAccountPayload,
    private val importSourceFactory: ImportSourceFactory,
) : BaseViewModel(),
    WithAccountNameChooserMixin {

    override val accountNameChooser: AccountNameChooserMixin.Presentation = accountNameChooserFactory.create(scope = this)

    val importSource = importSourceFactory.create(
        importType = payload.importType,
        scope = this,
        payload = payload.addAccountPayload,
        accountNameChooserMixin = accountNameChooser,
        coroutineScope = viewModelScope
    )

    private val importInProgressFlow = MutableStateFlow(false)

    private val nextButtonEnabledFlow = combine(
        importSource.fieldsValidFlow,
        accountNameChooser.nameValid,
    ) { fieldsValid, nameValid -> fieldsValid and nameValid }

    val nextButtonState = nextButtonEnabledFlow.combine(importInProgressFlow) { enabled, inProgress ->
        when {
            inProgress -> ButtonState.PROGRESS
            enabled -> ButtonState.NORMAL
            else -> ButtonState.DISABLED
        }
    }

    fun homeButtonClicked() {
        router.back()
    }

    fun optionsClicked() {
        router.openAdvancedSettings(AdvancedEncryptionModePayload.Change(payload.addAccountPayload))
    }

    fun nextClicked() = launch {
        importInProgressFlow.withFlagSet {
            val nameState = accountNameChooser.nameState.value
            val addAccountType = mapAddAccountPayloadToAddAccountType(payload.addAccountPayload, nameState)

            importSource.performImport(addAccountType)
                .onSuccess { continueBasedOnCodeStatus() }
                .onFailure(::handleCreateAccountError)
        }
    }

    fun systemCallResultReceived(requestCode: Int, intent: Intent) {
        if (importSource is FileRequester) {
            val currentRequestCode = importSource.chooseJsonFileEvent.value!!.peekContent()

            if (requestCode == currentRequestCode) {
                importSource.fileChosen(intent.data!!)
            }
        }
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (interactor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }

    private fun handleCreateAccountError(throwable: Throwable) {
        var errorMessage = importSource.handleError(throwable)

        if (errorMessage == null) {
            errorMessage = when (throwable) {
                is AccountAlreadyExistsException -> ImportError(
                    titleRes = R.string.account_add_already_exists_message,
                    messageRes = R.string.account_error_try_another_one
                )

                is JunctionDecoder.DecodingError, is BIP32JunctionDecoder.DecodingError -> ImportError(
                    titleRes = R.string.account_invalid_derivation_path_title,
                    messageRes = R.string.account_invalid_derivation_path_message_v2_2_0
                )

                else -> ImportError()
            }
        }

        val title = resourceManager.getString(errorMessage.titleRes)
        val message = resourceManager.getString(errorMessage.messageRes)

        showError(title, message)
    }
}
