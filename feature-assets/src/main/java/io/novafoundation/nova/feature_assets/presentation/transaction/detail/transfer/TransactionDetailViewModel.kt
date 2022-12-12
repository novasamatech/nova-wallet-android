package io.novafoundation.nova.feature_assets.presentation.transaction.detail.transfer

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.OptionalAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createOptionalAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class TransactionDetailViewModel(
    private val router: AssetsRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val chainRegistry: ChainRegistry,
    val operation: OperationParcelizeModel.Transfer,
    private val externalActions: ExternalActions.Presentation,
) : BaseViewModel(),
    ExternalActions by externalActions {

    private val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    val recipientAddressModelFlow = flowOf {
        getIcon(operation.receiver)
    }
        .inBackground()
        .share()

    val senderAddressModelLiveData = flowOf {
        getIcon(operation.sender)
    }
        .inBackground()
        .share()

    val chainUi = flowOf {
        mapChainToUi(chain())
    }
        .inBackground()
        .share()

    fun backClicked() {
        router.back()
    }

    private suspend fun getIcon(address: String): OptionalAddressModel {
        return addressIconGenerator.createOptionalAddressModel(
            chain = chain(),
            address = address,
            sizeInDp = AddressIconGenerator.SIZE_BIG,
            addressDisplayUseCase = addressDisplayUseCase,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )
    }

    fun repeatTransaction() {
        val retryAddress = if (operation.isIncome) operation.sender else operation.receiver

        router.openSend(AssetPayload(operation.chainId, operation.assetId), initialRecipientAddress = retryAddress)
    }

    fun transactionHashClicked() = operation.hash?.let {
        showExternalActions(ExternalActions.Type.Extrinsic(it))
    }

    fun fromAddressClicked() {
        showExternalActions(ExternalActions.Type.Address(operation.sender))
    }

    fun toAddressClicked() {
        showExternalActions(ExternalActions.Type.Address(operation.receiver))
    }

    private fun showExternalActions(type: ExternalActions.Type) = launch {
        externalActions.showExternalActions(type, chain())
    }
}
