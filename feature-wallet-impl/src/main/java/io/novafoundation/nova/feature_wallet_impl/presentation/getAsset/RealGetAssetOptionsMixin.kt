package io.novafoundation.nova.feature_wallet_impl.presentation.getAsset

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.AssetGetOptionsUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetBottomSheet
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class RealGetAssetOptionsMixinFactory(
    private val assetGetOptionsUseCase: AssetGetOptionsUseCase,
    private val walletRouter: WalletRouter,
    private val resourceManager: ResourceManager,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val chainRegistry: ChainRegistry,
    private val actionAwaitableFactory: ActionAwaitableMixin.Factory,
) : GetAssetOptionsMixin.Factory {

    override fun create(
        assetFlow: Flow<Chain.Asset?>,
        scope: CoroutineScope,
        additionalButtonFilter: Flow<Boolean>,
        showWhenNoOptions: Boolean
    ): GetAssetOptionsMixin {
        return RealGetAssetOptionsMixin(
            assetFlow,
            assetGetOptionsUseCase,
            walletRouter,
            resourceManager,
            selectedAccountUseCase,
            chainRegistry,
            actionAwaitableFactory,
            additionalButtonFilter,
            scope,
            showWhenNoOptions
        )
    }
}

class RealGetAssetOptionsMixin(
    private val assetFlow: Flow<Chain.Asset?>,
    private val assetGetOptionsUseCase: AssetGetOptionsUseCase,
    private val walletRouter: WalletRouter,
    private val resourceManager: ResourceManager,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val chainRegistry: ChainRegistry,
    actionAwaitableFactory: ActionAwaitableMixin.Factory,
    additionalButtonFilter: Flow<Boolean>,
    scope: CoroutineScope,
    showWhenNoOptions: Boolean
) : GetAssetOptionsMixin, CoroutineScope by scope {

    // Keep options tied to the asset that produced them, including after a late asset load or an asset change.
    private val getAssetPayloadFlow = assetFlow
        .distinctUntilChanged()
        .flatMapLatest { asset ->
            assetGetOptionsUseCase.observeAssetGetOptionsForSelectedAccount(flowOf(asset)).map { options ->
                asset?.let { GetAssetBottomSheet.Payload(it, options) }
            }
        }

    override val getAssetOptionsButtonState = combine(
        getAssetPayloadFlow,
        additionalButtonFilter
    ) { payload, shouldShownButton ->
        if (payload != null && shouldShowGetAssetButton(shouldShownButton, payload.availableOptions, showWhenNoOptions)) {
            val symbol = payload.chainAsset.symbol
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_get_token_format, symbol))
        } else {
            DescriptiveButtonState.Gone
        }
    }
        .onStart { emit(DescriptiveButtonState.Gone) }
        .distinctUntilChanged()
        .shareInBackground()

    override val observeGetAssetAction = actionAwaitableFactory.create<GetAssetBottomSheet.Payload, GetAssetOption>()

    override fun openAssetOptions() = launchUnit {
        // Read fresh options rather than the button's replay from before the asset became available.
        val payload = getAssetPayloadFlow.filterNotNull().first()
        val selectedOption = observeGetAssetAction.awaitAction(payload)
        onAssetOptionSelected(selectedOption, payload.chainAsset)
    }

    private fun onAssetOptionSelected(option: GetAssetOption, chainAsset: Chain.Asset) {
        when (option) {
            GetAssetOption.Receive -> receiveSelected(chainAsset)
            GetAssetOption.CrossChain -> onCrossChainTransferSelected(chainAsset)
            GetAssetOption.Buy -> buySelected(chainAsset)
            is GetAssetOption.DApp -> walletRouter.openDAppBrowser(option.url)
        }
    }

    private fun onCrossChainTransferSelected(chainAssetIn: Chain.Asset) = launch {
        val assetInChain = chainRegistry.getChain(chainAssetIn.chainId)

        val currentAddress = selectedAccountUseCase.getSelectedMetaAccount().addressIn(assetInChain)

        walletRouter.openSendCrossChain(AssetPayload(chainAssetIn.chainId, chainAssetIn.id), currentAddress)
    }

    private fun buySelected(chainAssetIn: Chain.Asset) {
        walletRouter.openBuyToken(chainAssetIn.chainId, chainAssetIn.id)
    }

    private fun receiveSelected(chainAssetIn: Chain.Asset) {
        walletRouter.openReceive(AssetPayload(chainAssetIn.chainId, chainAssetIn.id))
    }
}

internal fun shouldShowGetAssetButton(
    shouldShow: Boolean,
    options: Set<GetAssetOption>,
    showWhenNoOptions: Boolean
): Boolean = shouldShow && (options.isNotEmpty() || showWhenNoOptions)
