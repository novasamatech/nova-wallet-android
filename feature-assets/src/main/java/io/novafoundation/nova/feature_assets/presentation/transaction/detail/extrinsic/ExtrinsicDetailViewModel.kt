package io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.ExtrinsicContentParcel
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.model.ExtrinsicContentModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.launch

class ExtrinsicDetailViewModel(
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry,
    private val router: AssetsRouter,
    val operation: OperationParcelizeModel.Extrinsic,
    private val externalActions: ExternalActions.Presentation,
    private val resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider
) : BaseViewModel(),
    ExternalActions by externalActions {

    private val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    private val chainAsset by lazyAsync {
        chainRegistry.asset(operation.chainId, operation.chainAssetId)
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

    val operationIcon = flowOf {
        assetIconProvider.getAssetIconOrFallback(chainAsset().icon, AssetIconMode.WHITE)
    }.shareInBackground()

    val content = flowOf {
        mapExtrinsicContentParcelToModel(operation.content)
    }.shareInBackground()

    fun transactionIdClicked(hash: String) = launch {
        externalActions.showExternalActions(ExternalActions.Type.Extrinsic(hash), chain())
    }

    fun fromAddressClicked() = addressClicked(operation.originAddress)

    fun addressClicked(address: String) = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain())
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun mapExtrinsicContentParcelToModel(parcel: ExtrinsicContentParcel): ExtrinsicContentModel {
        val blocks = parcel.blocks.map { mapBlockFromParcel(it) }

        return ExtrinsicContentModel(blocks)
    }

    private suspend fun mapBlockFromParcel(block: ExtrinsicContentParcel.Block): ExtrinsicContentModel.Block {
        val entries = block.entries.map { mapBlockEntryFromParcel(it) }

        return ExtrinsicContentModel.Block(entries)
    }

    private suspend fun mapBlockEntryFromParcel(blockEntry: ExtrinsicContentParcel.BlockEntry): ExtrinsicContentModel.BlockEntry {
        return when (blockEntry) {
            is ExtrinsicContentParcel.BlockEntry.Address -> ExtrinsicContentModel.BlockEntry.Address(
                label = blockEntry.label,
                addressModel = getIcon(blockEntry.address),
            )

            is ExtrinsicContentParcel.BlockEntry.LabeledValue -> ExtrinsicContentModel.BlockEntry.LabeledValue(
                label = blockEntry.label,
                value = blockEntry.value
            )

            is ExtrinsicContentParcel.BlockEntry.TransactionId -> ExtrinsicContentModel.BlockEntry.TransactionId(
                label = resourceManager.getString(R.string.common_transaction_id),
                hash = blockEntry.hash
            )
        }
    }

    private suspend fun getIcon(address: String) = addressIconGenerator.createAddressModel(
        chain = chain(),
        address = address,
        sizeInDp = AddressIconGenerator.SIZE_BIG,
        addressDisplayUseCase = addressDisplayUseCase,
        background = AddressIconGenerator.BACKGROUND_TRANSPARENT
    )
}
