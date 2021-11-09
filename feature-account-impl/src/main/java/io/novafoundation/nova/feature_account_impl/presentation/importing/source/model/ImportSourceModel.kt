package io.novafoundation.nova.feature_account_impl.presentation.importing.source.model

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.isNotEmpty
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_account_api.domain.model.ImportJsonMetaData
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeModel
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.common.accountSource.AccountSource
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.importing.FileReader
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecodingException.IncorrectPasswordException
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecodingException.InvalidJsonException
import jp.co.soramitsu.fearless_utils.exceptions.Bip39Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bouncycastle.util.encoders.DecoderException

class ImportError(
    @StringRes val titleRes: Int = R.string.common_error_general_title,
    @StringRes val messageRes: Int = R.string.common_undefined_error_message
)

sealed class ImportSource(@StringRes nameRes: Int) : AccountSource(nameRes) {

    private val _validationLiveData = MediatorLiveData<Boolean>()
    val validationLiveData: LiveData<Boolean> = _validationLiveData

    init {
        _validationLiveData.value = false
    }

    abstract fun isFieldsValid(): Boolean

    open fun handleError(throwable: Throwable): ImportError? = null

    protected fun addValidationSource(liveData: LiveData<*>) {
        _validationLiveData.addSource(liveData) {
            _validationLiveData.value = isFieldsValid()
        }
    }
}

private const val PICK_FILE_RESULT_CODE = 101

class JsonImportSource(
    private val accountNameChooserMixin: AccountNameChooserMixin,
    private val cryptoTypeChooserMixin: CryptoTypeChooserMixin,
    private val addAccountInteractor: AddAccountInteractor,
    private val resourceManager: ResourceManager,
    private val clipboardManager: ClipboardManager,
    private val fileReader: FileReader,
    private val scope: CoroutineScope,
    private val payload: AddAccountPayload,
) : ImportSource(R.string.recovery_json), FileRequester {

    val jsonContentLiveData = MutableLiveData<String>()
    val passwordLiveData = MutableLiveData<String>()

    private val _showJsonInputOptionsEvent = MutableLiveData<Event<Unit>>()
    val showJsonInputOptionsEvent: LiveData<Event<Unit>> = _showJsonInputOptionsEvent

    private val _showNetworkWarningLiveData = MutableLiveData(false)
    val showNetworkWarningLiveData = _showNetworkWarningLiveData

    override val chooseJsonFileEvent = MutableLiveData<Event<RequestCode>>()

    init {
        addValidationSource(jsonContentLiveData)

        addValidationSource(passwordLiveData)
    }

    override fun isFieldsValid(): Boolean {
        return jsonContentLiveData.isNotEmpty() && passwordLiveData.isNotEmpty()
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
        clipboardManager.getFromClipboard()?.let(this::jsonReceived)
    }

    private fun jsonReceived(newJson: String) {
        jsonContentLiveData.value = newJson

        scope.launch {
            val result = addAccountInteractor.extractJsonMetadata(newJson)

            if (result.isSuccess) {
                handleParsedImportData(result.getOrThrow())
            }
        }
    }

    private fun handleParsedImportData(importJsonMetaData: ImportJsonMetaData) {
        showNetworkWarningLiveData.value = showShowNetworkWarning(importJsonMetaData.chainId)

        val cryptoModel = mapCryptoTypeToCryptoTypeModel(resourceManager, importJsonMetaData.encryptionType)
        cryptoTypeChooserMixin.selectedEncryptionChanged(cryptoModel)

        importJsonMetaData.name?.let(accountNameChooserMixin::nameChanged)
    }

    private fun showShowNetworkWarning(jsonChainId: String?): Boolean {
        val forcedChainId = (payload as? AddAccountPayload.ChainAccount)?.chainId

        // show warning if supplied json has network different than chain which account is creating for
        return jsonChainId != null && forcedChainId != null && jsonChainId != forcedChainId
    }
}

class MnemonicImportSource : ImportSource(R.string.recovery_passphrase) {

    val mnemonicContentLiveData = MutableLiveData<String>()

    override fun isFieldsValid() = mnemonicContentLiveData.isNotEmpty()

    init {
        addValidationSource(mnemonicContentLiveData)
    }

    override fun handleError(throwable: Throwable): ImportError? {
        return when (throwable) {
            is Bip39Exception -> ImportError(
                titleRes = R.string.import_mnemonic_invalid_title,
                messageRes = R.string.mnemonic_error_try_another_one
            )
            else -> null
        }
    }
}

class RawSeedImportSource : ImportSource(R.string.recovery_raw_seed) {

    val rawSeedLiveData = MutableLiveData<String>()

    override fun isFieldsValid() = rawSeedLiveData.isNotEmpty()

    override fun handleError(throwable: Throwable): ImportError? {
        return when (throwable) {
            is IllegalArgumentException, is DecoderException -> ImportError(
                titleRes = R.string.import_seed_invalid_title,
                messageRes = R.string.account_import_invalid_seed
            )
            else -> null
        }
    }

    init {
        addValidationSource(rawSeedLiveData)
    }
}
