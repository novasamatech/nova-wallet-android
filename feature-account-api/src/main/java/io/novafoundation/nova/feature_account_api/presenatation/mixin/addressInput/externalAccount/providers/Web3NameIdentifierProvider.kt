package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.providers

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.emitLoaded
import io.novafoundation.nova.common.domain.emitLoading
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.removeSpacing
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.AccountIdentifierProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.AccountIdentifierProvider.Event.ErrorEvent
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.AccountIdentifierProvider.Event.ShowBottomSheetEvent
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.ExternalAccount
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpecProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.toIdenticonState
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class Web3NameIdentifierProvider(
    private val web3NameInteractor: Web3NamesInteractor,
    private val destinationChain: Flow<ChainWithAsset>,
    private val addressInputSpecProvider: AddressInputSpecProvider,
    private val coroutineScope: CoroutineScope,
    private val input: Flow<String>,
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

    override val eventsLiveData = MutableLiveData<Event<AccountIdentifierProvider.Event>>()

    init {
        input.onEach {
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

        cancelLoadingJob()

        externalAccountsLoadingJob = launch {
            try {
                startLoading()

                val chain = destinationChain.first().chain
                runCatching { getExternalAccounts(raw) }
                    .onSuccess { onExternalAccountsLoaded(raw, chain.name, it) }
                    .onFailure { onError(it, chain.name, raw) }

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
                    address = it.address,
                    description = it.description,
                    addressWithDescription = resourceManager.addressWithDescription(it),
                    isValid = it.isValid,
                    icon = inputSpec.generateIcon(it.address).toIdenticonState()
                )
            }
    }

    private fun ResourceManager.addressWithDescription(w3nAccount: Web3NameAccount): String {
        val description = w3nAccount.description
        return if (description != null) {
            return getString(R.string.web3names_address_with_description, w3nAccount.address.ellipsizeAddress(), description)
        } else {
            w3nAccount.address.ellipsizeAddress()
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
                w3nIdentifier.removeSpacing(),
                chainName,
                externalAccounts,
                _selectedExternalAccountFlow.value
            ).event()
        }
    }

    private fun onError(throwable: Throwable, chainName: String, web3NameInput: String) {
        if (throwable is CancellationException) return

        if (throwable !is Web3NamesException) {
            eventsLiveData.value = ErrorEvent(Web3NamesException.UnknownException(web3NameInput, chainName)).event()
        } else {
            eventsLiveData.value = ErrorEvent(throwable).event()
        }
    }
}
