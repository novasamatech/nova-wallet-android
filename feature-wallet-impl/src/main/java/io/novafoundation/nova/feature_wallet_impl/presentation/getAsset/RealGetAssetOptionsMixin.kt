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
        additionalButtonFilter: Flow<Boolean>
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
            scope
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
    scope: CoroutineScope
) : GetAssetOptionsMixin, CoroutineScope by scope {

    private val getAssetOptionsFlow = assetGetOptionsUseCase.observeAssetGetOptionsForSelectedAccount(assetFlow)
        .shareInBackground()

    override val getAssetOptionsButtonState = combine(
        assetFlow.filterNotNull(),
        getAssetOptionsFlow,
        additionalButtonFilter
    ) { assetIn, getAssetInOptions, shouldShownButton ->
        if (shouldShownButton && getAssetInOptions.isNotEmpty()) {
            val symbol = assetIn.symbol
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
        val assetIn = assetFlow.first() ?: return@launchUnit
        val availableOptions = getAssetOptionsFlow.first()

        val payload = GetAssetBottomSheet.Payload(
            chainAsset = assetIn,
            availableOptions = availableOptions
        )

        val selectedOption = observeGetAssetAction.awaitAction(payload)
        onAssetOptionSelected(selectedOption)
    }

    private fun onAssetOptionSelected(option: GetAssetOption) {
        when (option) {
            GetAssetOption.RECEIVE -> receiveSelected()
            GetAssetOption.CROSS_CHAIN -> onCrossChainTransferSelected()
            GetAssetOption.BUY -> buySelected()
        }
    }

    private fun onCrossChainTransferSelected() = launch {
        val chainAssetIn = assetFlow.first() ?: return@launch
        val assetInChain = chainRegistry.getChain(chainAssetIn.chainId)

        val currentAddress = selectedAccountUseCase.getSelectedMetaAccount().addressIn(assetInChain)

        walletRouter.openSendCrossChain(AssetPayload(chainAssetIn.chainId, chainAssetIn.id), currentAddress)
    }

    private fun buySelected() = launch {
        val chainAssetIn = assetFlow.first() ?: return@launch
        walletRouter.openBuyToken(chainAssetIn.chainId, chainAssetIn.id)
    }

    private fun receiveSelected() = launch {
        val chainAssetIn = assetFlow.first() ?: return@launch
        walletRouter.openReceive(AssetPayload(chainAssetIn.chainId, chainAssetIn.id))
    }
}
