package io.novafoundation.nova.feature_account_impl.presentation.importing.source

import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionRequester
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.importing.FileReader
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.JsonImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.MnemonicImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.RawSeedImportSource
import kotlinx.coroutines.CoroutineScope

class ImportSourceFactory(
    private val addAccountInteractor: AddAccountInteractor,
    private val clipboardManager: ClipboardManager,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionRequester: AdvancedEncryptionRequester,
    private val fileReader: FileReader,
) {

    fun create(
        importType: ImportType,
        scope: CoroutineScope,
        payload: AddAccountPayload,
        accountNameChooserMixin: AccountNameChooserMixin,
    ): ImportSource {
        return when (importType) {
            ImportType.MNEMONIC -> MnemonicImportSource(
                addAccountInteractor = addAccountInteractor,
                addAccountPayload = payload,
                advancedEncryptionInteractor = advancedEncryptionInteractor,
                advancedEncryptionCommunicator = advancedEncryptionRequester
            )
            ImportType.SEED -> RawSeedImportSource(
                addAccountInteractor = addAccountInteractor,
                addAccountPayload = payload,
                advancedEncryptionInteractor = advancedEncryptionInteractor,
                advancedEncryptionCommunicator = advancedEncryptionRequester
            )
            ImportType.JSON -> JsonImportSource(
                accountNameChooserMixin = accountNameChooserMixin,
                addAccountInteractor = addAccountInteractor,
                clipboardManager = clipboardManager,
                fileReader = fileReader,
                scope = scope,
                payload = payload
            )
        }
    }
}
