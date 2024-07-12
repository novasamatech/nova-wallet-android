package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
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
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.CustomNetworkPayload
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.CustomNetworkValidationSystem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AddNetworkViewModel(
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter,
    private val payload: AddNetworkPayload,
    private val interactor: AddNetworkInteractor,
    private val validationExecutor: ValidationExecutor,
    private val autofillNetworkMetadataMixinFactory: AutofillNetworkMetadataMixinFactory
) : BaseViewModel(), Validatable by validationExecutor {

    val isChainIdVisibleFlow = flowOf { chainIdRequired() }
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
        val chainIdRequiredAndEmpty = chainIdRequired() && chainId.isBlank()
        when {
            isLoading -> DescriptiveButtonState.Loading
            url.isBlank() || networkName.isBlank() || tokenName.isBlank() || chainIdRequiredAndEmpty -> DescriptiveButtonState.Disabled(
                resourceManager.getString(R.string.common_enter_details)
            )

            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_add_network))
        }
    }

    init {
        val prefilledData = payload.prefilledData
        if (prefilledData != null) {
            nodeUrlFlow.value = prefilledData.rpcNodeUrl ?: ""
            networkNameFlow.value = prefilledData.networkName ?: ""
            tokenSymbolFlow.value = prefilledData.tokenName ?: ""
            evmChainIdFlow.value = prefilledData.evmChainId?.format() ?: ""
            blockExplorerFlow.value = prefilledData.blockExplorerUrl ?: ""
            priceProviderFlow.value = prefilledData.coingeckoLink ?: ""
        }

        runAutofill()
    }

    fun addNetworkClicked() {
        launch {
            val validationPayload = CustomNetworkPayload(
                nodeUrl = nodeUrlFlow.value,
                chainName = networkNameFlow.value,
                tokenSymbol = tokenSymbolFlow.value,
                evmChainId = evmChainIdFlow.value.toIntOrNull(),
                blockExplorerUrl = blockExplorerFlow.value.nullIfBlank(),
                coingeckoLinkUrl = priceProviderFlow.value.nullIfBlank(),
                ignoreChainModifying = false
            )

            validationExecutor.requireValid(
                validationSystem = getValidationSystem(),
                payload = validationPayload,
                progressConsumer = loadingState.progressConsumer(),
                validationFailureTransformerCustom = { status, actions -> mapSaveCustomNetworkFailureToUI(resourceManager, status, actions) }
            ) {
                launch {
                    executeSaving(validationPayload)
                    loadingState.value = false
                }
            }
        }
    }

    private suspend fun executeSaving(savingPayload: CustomNetworkPayload) {
        val nodeName = resourceManager.getString(R.string.create_network_node_name, savingPayload.chainName)
        val blockExplorerName = resourceManager.getString(R.string.create_network_block_explorer_name, savingPayload.chainName)
        val blockExplorerNameAndUrl = savingPayload.blockExplorerUrl?.let { blockExplorerName to it }

        val result = when (payload.mode) {
            AddNetworkPayload.Mode.EVM -> interactor.createEvmNetwork(
                chainId = savingPayload.evmChainId!!,
                iconUrl = payload.prefilledData?.iconUrl,
                nodeUrl = savingPayload.nodeUrl,
                nodeName = nodeName,
                chainName = savingPayload.chainName,
                tokenSymbol = savingPayload.tokenSymbol,
                blockExplorer = blockExplorerNameAndUrl,
                coingeckoLink = savingPayload.coingeckoLinkUrl
            )

            AddNetworkPayload.Mode.SUBSTRATE -> interactor.createSubstrateNetwork(
                iconUrl = payload.prefilledData?.iconUrl,
                nodeUrl = savingPayload.nodeUrl,
                nodeName = nodeName,
                chainName = savingPayload.chainName,
                tokenSymbol = savingPayload.tokenSymbol,
                blockExplorer = blockExplorerNameAndUrl,
                coingeckoLink = savingPayload.coingeckoLinkUrl,
                coroutineScope = viewModelScope
            )
        }

        result.onSuccess { router.finishCreateNetworkFlow() }
            .onFailure {
                Log.e(LOG_TAG, "Failed to save network", it)
                showError(resourceManager.getString(R.string.common_something_went_wrong_title))
            }
    }

    private fun getValidationSystem(): CustomNetworkValidationSystem {
        return when (payload.mode) {
            AddNetworkPayload.Mode.EVM -> interactor.getEvmValidationSystem(viewModelScope)
            AddNetworkPayload.Mode.SUBSTRATE -> interactor.getSubstrateValidationSystem(viewModelScope)
        }
    }

    private fun runAutofill() {
        val autofillMixin = when (payload.mode) {
            AddNetworkPayload.Mode.EVM -> autofillNetworkMetadataMixinFactory.evm(viewModelScope)
            AddNetworkPayload.Mode.SUBSTRATE -> autofillNetworkMetadataMixinFactory.substrate(viewModelScope)
        }

        nodeUrlFlow
            .drop(1)
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

    private fun chainIdRequired(): Boolean {
        return payload.mode == AddNetworkPayload.Mode.EVM
    }
}
