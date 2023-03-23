package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.emitError
import io.novafoundation.nova.common.domain.emitLoaded
import io.novafoundation.nova.common.domain.emitLoading
import io.novafoundation.nova.common.domain.loadedNothing
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.inputSpec.AddressInputSpecProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.web3names.domain.models.Web3NameAccount
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface AccountIdentifierProvider {

    val externalAccountsFlow: Flow<List<ExternalAccount>>

    val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>>

    fun init(inputFlow: Flow<String>)

    fun selectExternalAccount(identifier: ExternalAccount?)

    fun isCorrectIdentifier(raw: String): Boolean

    fun loadExternalAccounts(raw: String)

    fun getIdentifier(): String?

    fun getSelectedExternalAccount(): ExternalAccount?

    fun isValidExternalAccount(externalAccount: ExternalAccount): Boolean
}

class Web3NameIdentifierProvider(
    private val web3NameInteractor: Web3NamesInteractor,
    private val destinationChain: Flow<ChainWithAsset>,
    private val addressInputSpecProvider: AddressInputSpecProvider,
    private val coroutineScope: CoroutineScope
) : AccountIdentifierProvider,
    CoroutineScope by coroutineScope {

    private var enteredIdentifier: String? = null
    private var selectedExternalAccount: ExternalAccount? = null

    override val externalAccountsFlow = MutableStateFlow<List<ExternalAccount>>(listOf())

    override val selectedExternalAccountFlow = MutableStateFlow<ExtendedLoadingState<ExternalAccount?>>(loadedNothing())

    override fun init(inputFlow: Flow<String>) {
        inputFlow.onEach {
            selectedExternalAccount = null
            enteredIdentifier = null
            selectedExternalAccountFlow.value = loadedNothing()
            externalAccountsFlow.value = emptyList()
        }.launchIn(this)
    }

    override fun selectExternalAccount(account: ExternalAccount?) {
        launch {
            if (account == null) {
                enteredIdentifier = null
                externalAccountsFlow.value = emptyList()
            }
            selectedExternalAccount = account
            selectedExternalAccountFlow.emitLoaded(account)
        }
    }

    override fun isCorrectIdentifier(raw: String): Boolean {
        return web3NameInteractor.isValidWeb3Name(raw)
    }

    override fun loadExternalAccounts(raw: String) {
        launch {
            if (web3NameInteractor.isValidWeb3Name(raw)) {
                enteredIdentifier = raw

                startLoading()

                getExternalAccounts(raw)
                    .onSuccess { onExternalAccountsLoaded(it) }
                    .onFailure { selectedExternalAccountFlow.emitError(it) }

                stopLoading()
            }
        }
    }

    override fun getIdentifier(): String? {
        return enteredIdentifier
    }

    override fun getSelectedExternalAccount(): ExternalAccount? {
        return selectedExternalAccount
    }

    override fun isValidExternalAccount(externalAccount: ExternalAccount): Boolean {
        return web3NameInteractor.isValidWeb3NameAccount(
            Web3NameAccount(
                externalAccount.accountId,
                externalAccount.address,
                externalAccount.description
            )
        )
    }

    private suspend fun getExternalAccounts(raw: String): Result<List<ExternalAccount>> {
        val inputSpec = addressInputSpecProvider.spec.first()
        val chainWithAsset = destinationChain.first()
        val chain = chainWithAsset.chain
        val asset = chainWithAsset.asset

        return web3NameInteractor.queryAccountsByWeb3Name(raw, chain, asset).map { accounts ->
            accounts.map {
                ExternalAccount(
                    it.accountId,
                    it.address,
                    it.description,
                    inputSpec.generateIcon(it.address).toIdenticonState()
                )
            }
        }
    }

    private suspend fun startLoading() {
        selectedExternalAccountFlow.emitLoading()
    }

    private suspend fun stopLoading() {
        selectedExternalAccountFlow.emitLoaded(selectedExternalAccount)
    }

    private fun onExternalAccountsLoaded(externalAccounts: List<ExternalAccount>) {
        externalAccountsFlow.value = externalAccounts

        if (externalAccounts.size == 1) {
            selectedExternalAccount = externalAccounts.first()
        }
    }
}
