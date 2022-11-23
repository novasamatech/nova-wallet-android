package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Disabled
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Enabled
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Loading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.tokens.add.AddTokensInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

class AddTokenEnterInfoViewModel(
    private val router: AssetsRouter,
    private val interactor: AddTokensInteractor,
    private val resourceManager: ResourceManager,
    private val payload: AddTokenEnterInfoPayload,
) : BaseViewModel() {

    val contractAddressInput = MutableStateFlow("")
    val symbolInput = MutableStateFlow("")
    val decimalsInput = MutableStateFlow("")
    val priceLinkInput = MutableStateFlow("")

    private val addingInProgressFlow = MutableStateFlow(false)

    val continueButtonState = combine(
        contractAddressInput,
        symbolInput,
        decimalsInput,
        addingInProgressFlow
    ) { contractAddress, symbol, decimals, addingInProgress ->
        when {
            addingInProgress -> Loading
            contractAddress.isEmpty() -> Disabled(resourceManager.getString(R.string.asset_add_token_enter_contract_address))
            symbol.isEmpty() -> Disabled(resourceManager.getString(R.string.asset_add_token_enter_symbol))
            decimals.isEmpty() -> Disabled(resourceManager.getString(R.string.asset_add_token_enter_decimals))
            else -> Enabled(resourceManager.getString(R.string.assets_add_token))
        }
    }

    init {
        autoFillFieldsBasedOnContractAddress()
    }

    private fun autoFillFieldsBasedOnContractAddress() {
        contractAddressInput
            .mapLatest { contractAddress ->
                Log.d(this@AddTokenEnterInfoViewModel.LOG_TAG, "Start fetching metadata")
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
        TODO("Not yet implemented")
    }
}
