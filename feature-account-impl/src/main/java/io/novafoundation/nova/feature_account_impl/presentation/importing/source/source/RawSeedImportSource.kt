package io.novafoundation.nova.feature_account_impl.presentation.importing.source.source

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.SeedImportView
import io.novafoundation.nova.feature_account_impl.presentation.seedScan.ScanSeedCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.seedScan.ScanSeedRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.bouncycastle.util.encoders.DecoderException

class RawSeedImportSource(
    private val addAccountInteractor: AddAccountInteractor,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionSelectionStoreProvider: AdvancedEncryptionSelectionStoreProvider,
    private val scanSeedRequester: ScanSeedRequester,
    private val coroutineScope: CoroutineScope
) : ImportSource(R.string.account_import_seed_title), CoroutineScope by coroutineScope {

    private val advancedEncryptionSelectionStore = async { advancedEncryptionSelectionStoreProvider.getSelectionStore(coroutineScope) }

    override val encryptionOptionsAvailable: Boolean = true

    val rawSeedFlow = MutableStateFlow("")

    override val fieldsValidFlow: Flow<Boolean> = rawSeedFlow.map { it.isNotEmpty() }

    init {
        scanSeedRequester.responseFlow
            .onEach {
                rawSeedFlow.value = it.secret
            }
            .launchIn(this)
    }

    override suspend fun performImport(addAccountType: AddAccountType): Result<Unit> {
        val advancedEncryption = advancedEncryptionSelectionStore().getCurrentSelection()
            ?: advancedEncryptionInteractor.getRecommendedAdvancedEncryption()

        return addAccountInteractor.importFromSecret(rawSeedFlow.value, advancedEncryption, addAccountType)
    }

    override fun initializeView(viewModel: ImportAccountViewModel, fragment: BaseFragment<*, *>): SeedImportView {
        return SeedImportView(fragment.requireContext()).apply {
            observeCommon(viewModel, fragment.viewLifecycleOwner)
            observeSource(this@RawSeedImportSource, fragment.viewLifecycleOwner)
            onScanClick {
                scanSeedRequester.openRequest(ScanSeedCommunicator.Request())
            }
        }
    }

    override fun handleError(throwable: Throwable): ImportError? {
        return when (throwable) {
            is IllegalArgumentException, is DecoderException -> ImportError(
                titleRes = R.string.import_seed_invalid_title,
                messageRes = R.string.account_import_invalid_seed
            )

            else -> null
        }
    }
}
