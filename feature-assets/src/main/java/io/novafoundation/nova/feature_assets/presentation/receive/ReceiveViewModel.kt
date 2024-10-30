package io.novafoundation.nova.feature_assets.presentation.receive

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.view.QrCodeModel
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
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val COLORED_OVERLAY_PADDING_DP = 0
private const val WHITE_OVERLAY_PADDING_DP = 8

class ReceiveViewModel(
    private val interactor: ReceiveInteractor,
    private val qrCodeGenerator: QrCodeGenerator,
    private val resourceManager: ResourceManager,
    private val assetPayload: AssetPayload,
    private val chainRegistry: ChainRegistry,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val router: AssetsRouter,
    private val clipboardManager: ClipboardManager
) : BaseViewModel() {

    private val selectedMetaAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val chainWithAssetAsync by lazyAsync {
        chainRegistry.chainWithAsset(assetPayload.chainId, assetPayload.chainAssetId)
    }

    val chainFlow = flowOf { mapChainToUi(chainWithAssetAsync().chain) }

    val titleFlow = flowOf {
        val (_, chainAsset) = chainWithAssetAsync()
        resourceManager.getString(R.string.wallet_asset_receive_token, chainAsset.symbol)
    }

    val subtitleFlow = flowOf {
        val (chain, chainAsset) = chainWithAssetAsync()
        resourceManager.getString(R.string.wallet_asset_receive_token_subtitle, chainAsset.symbol, chain.name)
    }

    val qrCodeFlow = flowOf {
        val assetIconMode = interactor.getAssetIconMode()
        val qrInput = interactor.getQrCodeSharingString(assetPayload.chainId)

        val (overlayPadding, overlayBackground) = when (assetIconMode) {
            AssetIconMode.COLORED -> COLORED_OVERLAY_PADDING_DP to null
            AssetIconMode.WHITE -> WHITE_OVERLAY_PADDING_DP to resourceManager.getDrawable(R.drawable.bg_common_circle)
        }

        QrCodeModel(
            qrCodeGenerator.generateQrCode(qrInput),
            overlayBackground,
            overlayPadding,
            chainWithAssetAsync().asset.icon()
        )
    }

    val accountNameFlow = selectedMetaAccountFlow.map { it.name }

    val addressFlow = selectedMetaAccountFlow.map { it.addressIn(chainWithAssetAsync().chain)!! }

    private val _shareEvent = MutableLiveData<Event<QrSharingPayload>>()
    val shareEvent: LiveData<Event<QrSharingPayload>> = _shareEvent

    fun copyAddressClicked() = launch {
        val accountAddress = addressFlow.first()
        clipboardManager.addToClipboard(accountAddress)

        showToast(resourceManager.getString(io.novafoundation.nova.common.R.string.common_copied))
    }

    fun backClicked() {
        router.back()
    }

    fun shareButtonClicked(qrBitmap: Bitmap) = launch {
        val address = addressFlow.first()
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
