package io.novafoundation.nova.feature_account_impl.presentation.importing.source.model

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.ImportJsonMetaData
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionRequester
import io.novafoundation.nova.feature_account_impl.presentation.common.accountSource.AccountSource
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.importing.FileReader
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.ImportSourceView
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.JsonImportView
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.MnemonicImportView
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.SeedImportView
import io.novafoundation.nova.feature_account_impl.presentation.lastAdvancedEncryptionOrDefault
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecodingException.IncorrectPasswordException
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecodingException.InvalidJsonException
import jp.co.soramitsu.fearless_utils.exceptions.Bip39Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.bouncycastle.util.encoders.DecoderException

class ImportError(
    @StringRes val titleRes: Int = R.string.common_error_general_title,
    @StringRes val messageRes: Int = R.string.common_undefined_error_message
)

sealed class ImportSource(@StringRes titleRes: Int) : AccountSource(titleRes) {

    abstract val encryptionOptionsAvailable: Boolean

    abstract val fieldsValidFlow: Flow<Boolean>

    abstract fun initializeView(viewModel: ImportAccountViewModel, fragment: BaseFragment<*>): ImportSourceView<*>

    abstract suspend fun performImport(addAccountType: AddAccountType): Result<Unit>

    open fun handleError(throwable: Throwable): ImportError? = null
}

private const val PICK_FILE_RESULT_CODE = 101

class JsonImportSource(
    private val accountNameChooserMixin: AccountNameChooserMixin,
    private val addAccountInteractor: AddAccountInteractor,
    private val clipboardManager: ClipboardManager,
    private val fileReader: FileReader,
    private val scope: CoroutineScope,
    private val payload: AddAccountPayload,
) : ImportSource(R.string.account_import_json_title),
    FileRequester,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(scope) {

    override val encryptionOptionsAvailable: Boolean = false

    val jsonContentFlow = singleReplaySharedFlow<String>()
    val passwordFlow = MutableStateFlow("")

    override val fieldsValidFlow: Flow<Boolean> = combine(jsonContentFlow, passwordFlow) { jsonContent, password ->
        jsonContent.isNotEmpty() && password.isNotEmpty()
    }
        .onStart { emit(false) }
        .share()

    private val _showJsonInputOptionsEvent = MutableLiveData<Event<Unit>>()
    val showJsonInputOptionsEvent: LiveData<Event<Unit>> = _showJsonInputOptionsEvent

    private val _showNetworkWarningFlow = MutableStateFlow(false)
    val showNetworkWarningFlow: Flow<Boolean> = _showNetworkWarningFlow

    override val chooseJsonFileEvent = MutableLiveData<Event<RequestCode>>()

    override fun initializeView(viewModel: ImportAccountViewModel, fragment: BaseFragment<*>): ImportSourceView<*> {
        return JsonImportView(fragment.requireContext()).apply {
            observeCommon(viewModel, fragment.viewLifecycleOwner)
            observeSource(this@JsonImportSource, fragment.viewLifecycleOwner)
        }
    }

    override suspend fun performImport(addAccountType: AddAccountType): Result<Unit> {
        return addAccountInteractor.importFromJson(jsonContentFlow.first(), passwordFlow.value, addAccountType)
    }

    override fun handleError(throwable: Throwable): ImportError? {
        return when (throwable) {
            is IncorrectPasswordException -> ImportError(
                titleRes = R.string.import_json_invalid_password_title,
                messageRes = R.string.import_json_invalid_password
            )
            is InvalidJsonException -> ImportError(
                titleRes = R.string.import_json_invalid_format_title,
                messageRes = R.string.import_json_invalid_format_message
            )
            is AccountSecretsFactory.SecretsError.NotValidEthereumCryptoType -> ImportError(
                titleRes = R.string.import_json_unsupported_crypto_title,
                messageRes = R.string.import_json_unsupported_ethereum_crypto_message
            )
            is AccountSecretsFactory.SecretsError.NotValidSubstrateCryptoType -> ImportError(
                titleRes = R.string.import_json_unsupported_crypto_title,
                messageRes = R.string.import_json_unsupported_substrate_crypto_message
            )
            else -> null
        }
    }

    override fun fileChosen(uri: Uri) {
        scope.launch {
            val content = fileReader.readFile(uri)!!

            jsonReceived(content)
        }
    }

    fun jsonClicked() {
        _showJsonInputOptionsEvent.sendEvent()
    }

    fun chooseFileClicked() {
        chooseJsonFileEvent.value = Event(PICK_FILE_RESULT_CODE)
    }

    fun pasteClicked() {
        clipboardManager.getTextOrNull()?.let(this::jsonReceived)
    }

    private fun jsonReceived(newJson: String) {
        scope.launch {
            jsonContentFlow.emit(newJson)

            val result = addAccountInteractor.extractJsonMetadata(newJson)

            if (result.isSuccess) {
                handleParsedImportData(result.getOrThrow())
            }
        }
    }

    private fun handleParsedImportData(importJsonMetaData: ImportJsonMetaData) {
        _showNetworkWarningFlow.value = showShowNetworkWarning(importJsonMetaData.chainId)

        importJsonMetaData.name?.let(accountNameChooserMixin::nameChanged)
    }

    private fun showShowNetworkWarning(jsonChainId: String?): Boolean {
        val forcedChainId = (payload as? AddAccountPayload.ChainAccount)?.chainId

        // show warning if supplied json has network different than chain which account is creating for
        return jsonChainId != null && forcedChainId != null && jsonChainId != forcedChainId
    }
}

class MnemonicImportSource(
    private val addAccountInteractor: AddAccountInteractor,
    private val addAccountPayload: AddAccountPayload,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionCommunicator: AdvancedEncryptionRequester
) : ImportSource(R.string.account_import_mnemonic_title) {

    override val encryptionOptionsAvailable: Boolean = true

    val mnemonicContentFlow = MutableStateFlow("")

    override val fieldsValidFlow: Flow<Boolean> = mnemonicContentFlow.map { it.isNotEmpty() }

    override fun initializeView(viewModel: ImportAccountViewModel, fragment: BaseFragment<*>): ImportSourceView<*> {
        return MnemonicImportView(fragment.requireContext()).apply {
            observeCommon(viewModel, fragment.viewLifecycleOwner)
            observeSource(this@MnemonicImportSource, fragment.viewLifecycleOwner)
        }
    }

    override suspend fun performImport(addAccountType: AddAccountType): Result<Unit> {
        val advancedEncryption = advancedEncryptionCommunicator.lastAdvancedEncryptionOrDefault(addAccountPayload, advancedEncryptionInteractor)

        return addAccountInteractor.importFromMnemonic(mnemonicContentFlow.value, advancedEncryption, addAccountType)
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

class RawSeedImportSource(
    private val addAccountInteractor: AddAccountInteractor,
    private val addAccountPayload: AddAccountPayload,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionCommunicator: AdvancedEncryptionRequester
) : ImportSource(R.string.account_import_seed_title) {

    override val encryptionOptionsAvailable: Boolean = true

    val rawSeedFlow = MutableStateFlow("")

    override val fieldsValidFlow: Flow<Boolean> = rawSeedFlow.map { it.isNotEmpty() }

    override suspend fun performImport(addAccountType: AddAccountType): Result<Unit> {
        val advancedEncryption = advancedEncryptionCommunicator.lastAdvancedEncryptionOrDefault(addAccountPayload, advancedEncryptionInteractor)

        return addAccountInteractor.importFromSeed(rawSeedFlow.value, advancedEncryption, addAccountType)
    }

    override fun initializeView(viewModel: ImportAccountViewModel, fragment: BaseFragment<*>): ImportSourceView<*> {
        return SeedImportView(fragment.requireContext()).apply {
            observeCommon(viewModel, fragment.viewLifecycleOwner)
            observeSource(this@RawSeedImportSource, fragment.viewLifecycleOwner)
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
