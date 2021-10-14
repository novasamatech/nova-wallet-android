package jp.co.soramitsu.feature_wallet_impl.presentation.receive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.createAddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.QrCodeGenerator
import jp.co.soramitsu.common.utils.flowOf
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.common.utils.requireException
import jp.co.soramitsu.common.utils.requireValue
import jp.co.soramitsu.common.utils.write
import jp.co.soramitsu.feature_account_api.domain.interfaces.SelectedAccountUseCase
import jp.co.soramitsu.feature_account_api.domain.model.addressIn
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletInteractor
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.feature_wallet_impl.presentation.AssetPayload
import jp.co.soramitsu.feature_wallet_impl.presentation.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.presentation.receive.model.QrSharingPayload
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val QR_TEMP_IMAGE_NAME = "address.png"

class ReceiveViewModel(
    private val interactor: WalletInteractor,
    private val qrCodeGenerator: QrCodeGenerator,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val externalActions: ExternalActions.Presentation,
    private val assetPayload: AssetPayload,
    private val chainRegistry: ChainRegistry,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val router: WalletRouter,
) : BaseViewModel(), ExternalActions by externalActions {

    val chain by lazyAsync {
        chainRegistry.getChain(assetPayload.chainId)
    }

    val qrBitmapFlow = flowOf {
        val qrString = interactor.getQrCodeSharingString(assetPayload.chainId)

        qrCodeGenerator.generateQrBitmap(qrString)
    }
        .inBackground()
        .share()

    val accountAddressModelFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .map {
            val address = it.addressIn(chain())!!

            addressIconGenerator.createAddressModel(address, AddressIconGenerator.SIZE_BIG, it.name)
        }
        .inBackground()
        .share()

    private val _shareEvent = MutableLiveData<Event<QrSharingPayload>>()
    val shareEvent: LiveData<Event<QrSharingPayload>> = _shareEvent

    fun recipientClicked() = launch {
        val accountAddress = accountAddressModelFlow.first().address

        externalActions.showExternalActions(ExternalActions.Type.Address(accountAddress), chain())
    }

    fun backClicked() {
        router.back()
    }

    fun shareButtonClicked() = launch {
        val qrBitmap = qrBitmapFlow.first()
        val address = accountAddressModelFlow.first().address

        viewModelScope.launch {
            val result = interactor.createFileInTempStorageAndRetrieveAsset(assetPayload.chainId, assetPayload.chainAssetId, QR_TEMP_IMAGE_NAME)

            if (result.isSuccess) {
                val (file, asset) = result.requireValue()

                file.write(qrBitmap)

                val message = generateMessage(chain(), asset.token.configuration, address)

                _shareEvent.value = Event(QrSharingPayload(file, message))
            } else {
                showError(result.requireException())
            }
        }
    }

    private fun generateMessage(chain: Chain, tokenType: Chain.Asset, address: String): String {
        return resourceManager.getString(R.string.wallet_receive_share_message).format(
            chain.name,
            tokenType.symbol
        ) + " " + address
    }
}
