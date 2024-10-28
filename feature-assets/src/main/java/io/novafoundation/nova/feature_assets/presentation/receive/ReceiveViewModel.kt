package io.novafoundation.nova.feature_assets.presentation.receive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.receive.ReceiveInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.receive.model.QrSharingPayload
import io.novafoundation.nova.feature_assets.presentation.receive.model.TokenReceiver
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReceiveViewModel(
    private val interactor: ReceiveInteractor,
    private val qrCodeGenerator: QrCodeGenerator,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val externalActions: ExternalActions.Presentation,
    private val assetPayload: AssetPayload,
    private val chainRegistry: ChainRegistry,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val router: AssetsRouter,
) : BaseViewModel(), ExternalActions by externalActions {

    private val selectedMetaAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()

    private val chainWithAssetAsync by lazyAsync {
        chainRegistry.chainWithAsset(assetPayload.chainId, assetPayload.chainAssetId)
    }

    val qrBitmapFlow = flowOf {
        val qrString = interactor.getQrCodeSharingString(assetPayload.chainId)

        qrCodeGenerator.generateQrBitmap(qrString)
    }
        .inBackground()
        .share()

    val receiver = selectedMetaAccountFlow
        .map {
            val (chain, chainAsset) = chainWithAssetAsync()
            val address = it.addressIn(chain)!!

            TokenReceiver(
                addressModel = addressIconGenerator.createAddressModel(chain, address, AddressIconGenerator.SIZE_BIG, it.name),
                chain = mapChainToUi(chain),
                chainAssetIcon = chainAsset.iconUrl
            )
        }
        .inBackground()
        .share()

    val toolbarTitle = flowOf {
        val (_, chainAsset) = chainWithAssetAsync()

        resourceManager.getString(R.string.wallet_asset_receive_token, chainAsset.symbol)
    }
        .inBackground()
        .share()

    private val _shareEvent = MutableLiveData<Event<QrSharingPayload>>()
    val shareEvent: LiveData<Event<QrSharingPayload>> = _shareEvent

    fun recipientClicked() = launch {
        val accountAddress = receiver.first().addressModel.address
        val (chain, _) = chainWithAssetAsync()

        externalActions.showExternalActions(ExternalActions.Type.Address(accountAddress), chain)
    }

    fun backClicked() {
        router.back()
    }

    fun shareButtonClicked() = launch {
        val qrBitmap = qrBitmapFlow.first()
        val address = receiver.first().addressModel.address
        val (chain, chainAsset) = chainWithAssetAsync()

        viewModelScope.launch {
            interactor.generateTempQrFile(qrBitmap)
                .onSuccess { fileUri ->
                    val message = generateShareMessage(chain, chainAsset, address)

                    _shareEvent.value = Event(QrSharingPayload(fileUri, message))
                }
                .onFailure(::showError)
        }
    }

    private fun generateShareMessage(chain: Chain, tokenType: Chain.Asset, address: String): String {
        return resourceManager.getString(R.string.wallet_receive_share_message).format(
            chain.name,
            tokenType.symbol
        ) + " " + address
    }
}
