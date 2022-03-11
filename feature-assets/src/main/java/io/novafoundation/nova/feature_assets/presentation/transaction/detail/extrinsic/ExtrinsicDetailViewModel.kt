package io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class ExtrinsicDetailViewModel(
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry,
    private val router: WalletRouter,
    val operation: OperationParcelizeModel.Extrinsic,
    private val externalActions: ExternalActions.Presentation
) : BaseViewModel(),
    ExternalActions by externalActions {

    private val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    val senderAddressModelFlow = flowOf {
        getIcon(operation.originAddress)
    }
        .inBackground()
        .share()

    val chainUi = flowOf {
        mapChainToUi(chain())
    }
        .inBackground()
        .share()

    private suspend fun getIcon(address: String) = addressIconGenerator.createAddressModel(
        chain = chain(),
        address = address,
        sizeInDp = AddressIconGenerator.SIZE_BIG,
        addressDisplayUseCase = addressDisplayUseCase,
        background = AddressIconGenerator.BACKGROUND_TRANSPARENT
    )

    fun extrinsicClicked() = launch {
        externalActions.showExternalActions(ExternalActions.Type.Extrinsic(operation.hash), chain())
    }

    fun fromAddressClicked() = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(operation.originAddress), chain())
    }

    fun backClicked() {
        router.back()
    }
}
