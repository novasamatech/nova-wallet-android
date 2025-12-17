package io.novafoundation.nova.feature_account_impl.presentation.importing.source.source

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType
import io.novafoundation.nova.feature_account_api.presenatation.account.common.model.toAdvancedEncryption
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.ImportSourceView
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.MnemonicImportView
import io.novasama.substrate_sdk_android.exceptions.Bip39Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MnemonicImportSource(
    private val addAccountInteractor: AddAccountInteractor,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionSelectionStoreProvider: AdvancedEncryptionSelectionStoreProvider,
    private val importType: ImportType.Mnemonic,
    private val coroutineScope: CoroutineScope
) : ImportSource(R.string.account_import_mnemonic_title), CoroutineScope by coroutineScope {

    private var advancedEncryptionSelectionStore = async { advancedEncryptionSelectionStoreProvider.getSelectionStore(coroutineScope) }

    override val encryptionOptionsAvailable: Boolean = importType.origin == ImportType.Mnemonic.Origin.DEFAULT

    val mnemonicContentFlow = MutableStateFlow("")

    override val fieldsValidFlow: Flow<Boolean> = mnemonicContentFlow.map { it.isNotEmpty() }

    init {
        launch {
            val preset = importType.preset
            if (preset != null) {
                val advancedSettings = preset.toAdvancedEncryption()
                advancedEncryptionSelectionStore().updateSelection(advancedSettings)
            }

            importType.mnemonic?.let { mnemonicContentFlow.value = it }
        }
    }

    override fun initializeView(viewModel: ImportAccountViewModel, fragment: BaseFragment<*, *>): ImportSourceView<*> {
        return MnemonicImportView(fragment.requireContext()).apply {
            observeCommon(viewModel, fragment.viewLifecycleOwner)
            observeSource(this@MnemonicImportSource, fragment.viewLifecycleOwner)
        }
    }

    override suspend fun performImport(addAccountType: AddAccountType): Result<Unit> {
        return when (importType.origin) {
            ImportType.Mnemonic.Origin.DEFAULT -> performImportFromDefaultOrigin(addAccountType)
            ImportType.Mnemonic.Origin.TRUST_WALLET -> performImportFromTrustWallet(addAccountType)
        }
    }

    private suspend fun performImportFromDefaultOrigin(addAccountType: AddAccountType): Result<Unit> {
        val advancedEncryption = advancedEncryptionSelectionStore().getCurrentSelection()
            ?: advancedEncryptionInteractor.getRecommendedAdvancedEncryption()

        return addAccountInteractor.importFromMnemonic(mnemonicContentFlow.value, advancedEncryption, addAccountType)
    }

    private suspend fun performImportFromTrustWallet(addAccountType: AddAccountType): Result<Unit> {
        require(addAccountType is AddAccountType.MetaAccount) { "Cannot import chain account from Trust Wallet passphrase" }

        return addAccountInteractor.importFromTrustWallet(mnemonicContentFlow.value, addAccountType)
    }

    override fun handleError(throwable: Throwable): ImportError? {
        return when (throwable) {
            is Bip39Exception -> ImportError(
                titleRes = R.string.import_mnemonic_invalid_title,
                messageRes = R.string.mnemonic_error_try_another_one_v2_2_0
            )

            else -> null
        }
    }
}
