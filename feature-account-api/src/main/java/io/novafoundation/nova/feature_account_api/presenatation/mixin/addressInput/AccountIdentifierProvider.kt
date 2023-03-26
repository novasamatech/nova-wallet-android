package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.emitLoaded
import io.novafoundation.nova.common.domain.emitLoading
import io.novafoundation.nova.common.domain.loadedNothing
import io.novafoundation.nova.common.presentation.toShortAddressFormat
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AccountIdentifierProvider.Event.ErrorEvent
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AccountIdentifierProvider.Event.ShowBottomSheetEvent
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpecProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException
import io.novafoundation.nova.web3names.domain.models.Web3NameAccount
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface AccountIdentifierProvider {

    val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>>

    val eventsLiveData: LiveData<Event>

    fun selectExternalAccount(account: ExternalAccount?)

    fun isIdentifierValid(raw: String): Boolean

    fun loadExternalAccounts(raw: String)

    sealed interface Event {

        class ShowBottomSheetEvent(
            val identifier: String,
            val chainName: String,
            val externalAccounts: List<ExternalAccount>,
            val selectedAccount: ExternalAccount?
        ) : Event

        class ErrorEvent(val exception: Throwable) : Event
    }
}

class EmptyAccountIdentifierProvider : AccountIdentifierProvider {

    override val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>> = flowOf(loadedNothing())

    override val eventsLiveData: LiveData<AccountIdentifierProvider.Event> = MutableLiveData()

    override fun selectExternalAccount(account: ExternalAccount?) {
        // empty implementation
    }

    override fun loadExternalAccounts(raw: String) {
        // empty implementation
    }

    override fun isIdentifierValid(raw: String) = false
}

class Web3NameIdentifierProvider(
    private val web3NameInteractor: Web3NamesInteractor,
    private val destinationChain: Flow<ChainWithAsset>,
    private val addressInputSpecProvider: AddressInputSpecProvider,
    private val coroutineScope: CoroutineScope,
    private val inputFlowProvider: InputFlowProvider,
    private val resourceManager: ResourceManager
) : AccountIdentifierProvider,
    CoroutineScope by coroutineScope {

    private var externalAccountsLoadingJob: Job? = null
    private val _selectedExternalAccountFlow = MutableStateFlow<ExternalAccount?>(null)
    private val _externalAccountsLoadingFlow = MutableStateFlow(false)

    override val selectedExternalAccountFlow = combineTransform<ExternalAccount?, Boolean, ExtendedLoadingState<ExternalAccount?>>(
        _selectedExternalAccountFlow,
        _externalAccountsLoadingFlow
    ) { selectedAccount, isLoading ->
        if (isLoading) {
            emitLoading()
        } else {
            emitLoaded(selectedAccount)
        }
    }

    override val eventsLiveData = MutableLiveData<AccountIdentifierProvider.Event>()

    init {
        inputFlowProvider.inputFlow.onEach {
            _selectedExternalAccountFlow.value = null
            cancelLoadingJob()
        }.launchIn(this)
    }

    override fun selectExternalAccount(account: ExternalAccount?) {
        _selectedExternalAccountFlow.value = account
    }

    override fun isIdentifierValid(raw: String): Boolean {
        return web3NameInteractor.isValidWeb3Name(raw)
    }

    override fun loadExternalAccounts(raw: String) {
        if (!web3NameInteractor.isValidWeb3Name(raw)) return

        externalAccountsLoadingJob = launch {
            try {
                startLoading()

                val chain = destinationChain.first().chain
                runCatching { getExternalAccounts(raw) }
                    .onSuccess { onExternalAccountsLoaded(raw, chain.name, it) }
                    .onFailure { onError(it, chain.name) }

                stopLoading()
            } catch (_: CancellationException) {
                // Just skip
            }
        }
    }

    private suspend fun getExternalAccounts(raw: String): List<ExternalAccount> {
        val inputSpec = addressInputSpecProvider.spec.first()
        val chainWithAsset = destinationChain.first()
        val chain = chainWithAsset.chain
        val asset = chainWithAsset.asset

        return web3NameInteractor.queryAccountsByWeb3Name(raw, chain, asset)
            .map {
                ExternalAccount(
                    accountId = it.accountId,
                    address = it.address,
                    description = it.description,
                    addressWithDescription = resourceManager.addressWithDescription(it),
                    isValid = it.isValid,
                    icon = inputSpec.generateIcon(it.address).toIdenticonState()
                )
            }
    }

    fun ResourceManager.addressWithDescription(w3nAccount: Web3NameAccount): String {
        val description = w3nAccount.description
        return if (description != null) {
            return getString(R.string.web3names_address_with_description, w3nAccount.address.toShortAddressFormat(), description)
        } else {
            w3nAccount.address.toShortAddressFormat()
        }
    }

    private fun startLoading() {
        _externalAccountsLoadingFlow.value = true
    }

    private fun stopLoading() {
        _externalAccountsLoadingFlow.value = false
        externalAccountsLoadingJob = null
    }

    private fun cancelLoadingJob() {
        externalAccountsLoadingJob?.cancel()
        stopLoading()
    }

    private fun onExternalAccountsLoaded(w3nIdentifier: String, chainName: String, externalAccounts: List<ExternalAccount>) {
        if (externalAccounts.size == 1) {
            _selectedExternalAccountFlow.value = externalAccounts.first()
        } else if (externalAccounts.size > 1) {
            eventsLiveData.value = ShowBottomSheetEvent(
                web3NameInteractor.removePrefix(w3nIdentifier),
                chainName,
                externalAccounts,
                _selectedExternalAccountFlow.value
            )
        }
    }

    private fun onError(throwable: Throwable, chainName: String) {
        if (throwable is CancellationException) return

        if (throwable !is Web3NamesException) {
            eventsLiveData.value = ErrorEvent(Web3NamesException.UnknownException(chainName))
        } else {
            eventsLiveData.value = ErrorEvent(throwable)
        }
    }
}
