package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.nullIfBlank
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.AddNetworkInteractor
import io.novafoundation.nova.feature_settings_impl.domain.model.CustomNetworkPayload
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.CustomNetworkValidationSystem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload.Mode
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.getChain
import io.novafoundation.nova.runtime.ext.evmChainIdOrNull
import io.novafoundation.nova.runtime.ext.networkType
import io.novafoundation.nova.runtime.ext.normalizedUrl
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AddNetworkViewModel(
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter,
    private val payload: AddNetworkPayload,
    private val interactor: AddNetworkInteractor,
    private val validationExecutor: ValidationExecutor,
    private val autofillNetworkMetadataMixinFactory: AutofillNetworkMetadataMixinFactory,
    private val coinGeckoLinkParser: CoinGeckoLinkParser,
    private val chainRegistry: ChainRegistry
) : BaseViewModel(), Validatable by validationExecutor {

    private val networkType = flowOf { getNetworkType() }
        .shareInBackground()

    val isChainIdVisibleFlow = flowOf { isChainIdFieldRequired() }
        .shareInBackground()

    val isNodeEditable = flowOf { payload.mode is Mode.Add }
        .shareInBackground()

    val nodeUrlFlow = MutableStateFlow("")
    val networkNameFlow = MutableStateFlow("")
    val tokenSymbolFlow = MutableStateFlow("")
    val evmChainIdFlow = MutableStateFlow("")
    val blockExplorerFlow = MutableStateFlow("")
    val priceProviderFlow = MutableStateFlow("")

    private val loadingState = MutableStateFlow(false)

    val buttonState = combine(
        nodeUrlFlow,
        networkNameFlow,
        tokenSymbolFlow,
        evmChainIdFlow,
        loadingState
    ) { url, networkName, tokenName, chainId, isLoading ->
        val chainIdRequiredAndEmpty = isChainIdFieldRequired() && chainId.isBlank()
        when {
            isLoading -> DescriptiveButtonState.Loading
            url.isBlank() || networkName.isBlank() || tokenName.isBlank() || chainIdRequiredAndEmpty -> DescriptiveButtonState.Disabled(
                resourceManager.getString(R.string.common_enter_details)
            )

            else -> {
                val resId = when (payload.mode) {
                    is Mode.Add -> R.string.common_add_network
                    is Mode.Edit -> R.string.common_save
                }
                DescriptiveButtonState.Enabled(resourceManager.getString(resId))
            }
        }
    }

    init {
        launch {
            prefillData()

            runAutofill()
        }
    }

    private suspend fun prefillData() {
        val mode = payload.mode
        val chain = when (mode) {
            is Mode.Add -> mode.getChain()
            is Mode.Edit -> chainRegistry.getChain(mode.chainId)
        }

        val asset = chain?.utilityAsset
        nodeUrlFlow.value = chain?.nodes?.nodes?.first()?.unformattedUrl.orEmpty()
        networkNameFlow.value = chain?.name.orEmpty()
        tokenSymbolFlow.value = asset?.symbol?.value.orEmpty()
        evmChainIdFlow.value = chain?.evmChainIdOrNull()?.format().orEmpty()
        blockExplorerFlow.value = chain?.explorers?.firstOrNull()?.normalizedUrl().orEmpty()
        priceProviderFlow.value = asset?.priceId?.let { coinGeckoLinkParser.format(it) }.orEmpty()
    }

    fun addNetworkClicked() {
        launch {
            val chainName = networkNameFlow.value
            val blockExplorerUrl = blockExplorerFlow.value.nullIfBlank()
            val blockExplorerName = resourceManager.getString(R.string.create_network_block_explorer_name, chainName)
            val validationPayload = CustomNetworkPayload(
                nodeUrl = nodeUrlFlow.value,
                nodeName = resourceManager.getString(R.string.create_network_node_name, chainName),
                chainName = chainName,
                tokenSymbol = tokenSymbolFlow.value,
                evmChainId = evmChainIdFlow.value.toIntOrNull(),
                blockExplorer = blockExplorerUrl?.let { CustomNetworkPayload.BlockExplorer(blockExplorerName, it) },
                coingeckoLinkUrl = priceProviderFlow.value.nullIfBlank(),
                ignoreChainModifying = payload.mode is Mode.Edit // Skip dialog about Chain modifying if it's already editing
            )

            validationExecutor.requireValid(
                validationSystem = getValidationSystem(),
                payload = validationPayload,
                progressConsumer = loadingState.progressConsumer(),
                validationFailureTransformerCustom = { status, actions ->
                    mapSaveCustomNetworkFailureToUI(
                        resourceManager,
                        status,
                        actions,
                        ::changeTokenSymbol
                    )
                }
            ) {
                executeSaving(validationPayload)
            }
        }
    }

    private fun executeSaving(savingPayload: CustomNetworkPayload) {
        launch {
            val result = when (payload.mode) {
                is Mode.Add -> createNetwork(payload.mode, savingPayload)
                is Mode.Edit -> editNetwork(payload.mode, savingPayload)
            }

            result.onSuccess { finishSavingFlow() }
                .onFailure {
                    Log.e(LOG_TAG, "Failed to save network", it)
                    showError(resourceManager.getString(R.string.common_something_went_wrong_title))
                }

            loadingState.value = false
        }
    }

    private fun finishSavingFlow() {
        launch {
            when (payload.mode) {
                is Mode.Add -> router.finishCreateNetworkFlow()
                is Mode.Edit -> router.back()
            }
        }
    }

    private suspend fun createNetwork(mode: Mode.Add, savingPayload: CustomNetworkPayload): Result<Unit> {
        return when (mode.networkType) {
            Mode.Add.NetworkType.EVM -> interactor.createEvmNetwork(savingPayload, mode.getChain())

            Mode.Add.NetworkType.SUBSTRATE -> interactor.createSubstrateNetwork(savingPayload, mode.getChain(), coroutineScope)
        }
    }

    private suspend fun editNetwork(mode: Mode.Edit, savingPayload: CustomNetworkPayload): Result<Unit> {
        return interactor.updateChain(
            mode.chainId,
            savingPayload.chainName,
            savingPayload.tokenSymbol,
            savingPayload.blockExplorer,
            savingPayload.coingeckoLinkUrl
        )
    }

    private fun changeTokenSymbol(symbol: String) {
        tokenSymbolFlow.value = symbol
    }

    private suspend fun getValidationSystem(): CustomNetworkValidationSystem {
        return when (networkType.first()) {
            NetworkType.EVM -> interactor.getEvmValidationSystem(viewModelScope)
            NetworkType.SUBSTRATE -> interactor.getSubstrateValidationSystem(viewModelScope)
        }
    }

    private fun runAutofill() {
        launch {
            val autofillMixin = when (networkType.first()) {
                NetworkType.EVM -> autofillNetworkMetadataMixinFactory.evm(viewModelScope)
                NetworkType.SUBSTRATE -> autofillNetworkMetadataMixinFactory.substrate(viewModelScope)
            }

            nodeUrlFlow
                .ignorePrefilledValues()
                .debounce(500.milliseconds)
                .mapLatest { url -> autofillMixin.autofill(url) }
                .onEach { result ->
                    result.onSuccess { data ->
                        data.chainName?.let { networkNameFlow.value = it }
                        data.tokenSymbol?.let { tokenSymbolFlow.value = it }
                        data.evmChainId?.let { evmChainIdFlow.value = it.toString() }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun <T> Flow<T>.ignorePrefilledValues(): Flow<T> {
        return this.drop(1)
    }

    private fun isChainIdFieldRequired(): Boolean {
        return payload.mode is Mode.Add && payload.mode.networkType == Mode.Add.NetworkType.EVM
    }

    private suspend fun getNetworkType(): NetworkType {
        return when (payload.mode) {
            is Mode.Add -> {
                when (payload.mode.networkType) {
                    Mode.Add.NetworkType.SUBSTRATE -> NetworkType.SUBSTRATE
                    Mode.Add.NetworkType.EVM -> NetworkType.EVM
                }
            }

            is Mode.Edit -> chainRegistry.getChain(payload.mode.chainId).networkType()
        }
    }
}
