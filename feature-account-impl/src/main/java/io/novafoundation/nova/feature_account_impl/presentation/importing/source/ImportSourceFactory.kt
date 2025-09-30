package io.novafoundation.nova.feature_account_impl.presentation.importing.source

import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.importing.FileReader
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.JsonImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.MnemonicImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.RawSeedImportSource
import kotlinx.coroutines.CoroutineScope

class ImportSourceFactory(
    private val addAccountInteractor: AddAccountInteractor,
    private val clipboardManager: ClipboardManager,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionSelectionStoreProvider: AdvancedEncryptionSelectionStoreProvider,
    private val fileReader: FileReader,
) {

    fun create(
        importType: ImportType,
        scope: CoroutineScope,
        payload: AddAccountPayload,
        accountNameChooserMixin: AccountNameChooserMixin,
        coroutineScope: CoroutineScope
    ): ImportSource {
        return when (importType) {
            is ImportType.Mnemonic -> MnemonicImportSource(
                addAccountInteractor = addAccountInteractor,
                advancedEncryptionInteractor = advancedEncryptionInteractor,
                advancedEncryptionSelectionStoreProvider = advancedEncryptionSelectionStoreProvider,
                importType = importType,
                coroutineScope = coroutineScope
            )

            ImportType.Seed -> RawSeedImportSource(
                addAccountInteractor = addAccountInteractor,
                addAccountPayload = payload,
                advancedEncryptionInteractor = advancedEncryptionInteractor,
                advancedEncryptionSelectionStoreProvider = advancedEncryptionSelectionStoreProvider,
                coroutineScope = coroutineScope
            )

            ImportType.Json -> JsonImportSource(
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
