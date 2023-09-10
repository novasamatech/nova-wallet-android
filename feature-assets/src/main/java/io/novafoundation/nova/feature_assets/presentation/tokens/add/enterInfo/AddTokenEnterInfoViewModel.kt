package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import android.text.InputType
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Disabled
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Enabled
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Loading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.tokens.add.AddTokensInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.add.CustomToken
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.AddTokenPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AddTokenEnterInfoViewModel(
    private val router: AssetsRouter,
    private val interactor: AddTokensInteractor,
    private val resourceManager: ResourceManager,
    private val payload: AddTokenEnterInfoPayload,
    private val validationExecutor: ValidationExecutor,
    private val chainRegistry: ChainRegistry,
) : BaseViewModel(), Validatable by validationExecutor {

    val chain = flowOf { chainRegistry.getChain(payload.chainId) }
    val titleResId = chain.map { chain ->
        if (chain.isEthereumBased) {
            R.string.assets_add_erc20_token_enter_info_title
        } else {
            R.string.assets_add_substrate_token_enter_info_title
        }
    }
    val tokenIdTitleResId = chain.map { chain ->
        if (chain.isEthereumBased) {
            R.string.assets_add_erc20_token_enter_info_address_label
        } else {
            R.string.assets_add_substrate_token_enter_info_address_label
        }
    }
    val tokenIdHintResId = chain.map { chain ->
        if (chain.isEthereumBased) {
            R.string.assets_add_erc20_token_enter_info_address_hint
        } else {
            R.string.assets_add_substrate_token_enter_info_address_hint
        }
    }
    val tokenIdInputType = chain.map { chain ->
        if (chain.isEthereumBased) {
            InputType.TYPE_CLASS_TEXT
        } else {
            InputType.TYPE_CLASS_NUMBER
        }
    }

    val contractAddressInput = MutableStateFlow("")
    val symbolInput = MutableStateFlow("")
    val decimalsInput = MutableStateFlow("")
    val priceLinkInput = MutableStateFlow("")

    private val intDecimals = decimalsInput
        .map { it.toIntOrNull() }

    private val addingInProgressFlow = MutableStateFlow(false)

    val continueButtonState = combine(
        contractAddressInput,
        symbolInput,
        intDecimals,
        addingInProgressFlow
    ) { contractAddress, symbol, decimals, addingInProgress ->
        when {
            addingInProgress -> Loading
            contractAddress.isEmpty() -> Disabled(resourceManager.getString(R.string.asset_add_token_enter_contract_address))
            symbol.isEmpty() -> Disabled(resourceManager.getString(R.string.asset_add_token_enter_symbol))
            decimals == null -> Disabled(resourceManager.getString(R.string.asset_add_token_enter_decimals))
            else -> Enabled(resourceManager.getString(R.string.assets_add_token))
        }
    }

    init {
        autocompleteFieldsBasedOnContractAddress()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun autocompleteFieldsBasedOnContractAddress() {
        contractAddressInput
            .mapLatest { contractAddress ->
                interactor.retrieveContractMetadata(payload.chainId, contractAddress)
            }
            .onEach { contractMetadata ->
                contractMetadata?.decimals?.let { decimalsInput.value = it.toString() }
                contractMetadata?.symbol?.let { symbolInput.value = it }
            }
            .launchIn(viewModelScope)
    }

    fun backClicked() {
        router.back()
    }

    fun confirmClicked() {
        launch {
            val customToken = CustomToken(
                contractAddressInput.first(),
                intDecimals.first()!!,
                symbolInput.first(),
                priceLinkInput.first(),
                payload.chainId
            )


            val payload = AddTokenPayload(
                customToken,
                chain = chain.first(),
            )

            validationExecutor.requireValid(
                validationSystem = interactor.getValidationSystem(payload.chain.isEthereumBased),
                payload = payload,
                progressConsumer = addingInProgressFlow.progressConsumer(),
                validationFailureTransformer = { mapAddEvmTokensValidationFailureToUI(resourceManager, it) }
            ) {
                performAddToken(it.customToken)
            }
        }
    }

    private fun performAddToken(customToken: CustomToken) {
        launch {
            runCatching {
                interactor.addCustomTokenAndSync(customToken)
            }.onSuccess {
                addingInProgressFlow.value = false
                router.finishAddTokenFlow()
            }.onFailure {
                showError(it)
            }
        }
    }
}
