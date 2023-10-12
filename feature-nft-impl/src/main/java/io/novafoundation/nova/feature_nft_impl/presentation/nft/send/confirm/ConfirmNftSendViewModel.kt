package io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferModel
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferPayload
import io.novafoundation.nova.feature_nft_impl.domain.nft.send.NftSendInteractor
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.NftTransferDraft
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.mapNftTransferValidationFailureToUI
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.mapToDomain
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class ConfirmNftSendViewModel(
    private val nftSendInteractor: NftSendInteractor,
    private val router: NftRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val validationExecutor: ValidationExecutor,
    private val walletUiUseCase: WalletUiUseCase,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    val transferDraft: NftTransferDraft,
    private val resourceManager: ResourceManager
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val originChain by lazyAsync { chainRegistry.getChain(transferDraft.chainId) }

    private val commissionAssetFlow = nftSendInteractor.commissionAssetFlow(transferDraft.chainId)
        .inBackground()
        .share()

    private val commissionTokenFlow = nftSendInteractor.commissionAssetFlow(transferDraft.chainId)
        .map { it.token }
        .inBackground()
        .share()

    val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionTokenFlow)

    private val currentAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    val recipientModel = flowOf {
        createAddressModel(
            address = transferDraft.recipientAddress,
            chain = originChain(),
            resolveName = true
        )
    }
        .inBackground()
        .share()

    val senderModel = currentAccount.mapLatest { metaAccount ->
        createAddressModel(
            address = metaAccount.requireAddressIn(originChain()),
            chain = originChain(),
            resolveName = false
        )
    }
        .inBackground()
        .share()

    val originChainUi = flowOf { originChain() }
        .map(::mapChainToUi)
        .shareInBackground()

    val wallet = walletUiUseCase.selectedWalletUiFlow()
        .inBackground()
        .share()

    private val _transferSubmittingFlow = MutableStateFlow(false)
    val sendButtonStateFlow = _transferSubmittingFlow.asStateFlow()

    init {
        setInitialState()
    }

    fun backClicked() {
        router.back()
    }

    fun recipientAddressClicked() = launch {
        showExternalActions(transferDraft.recipientAddress, originChain)
    }

    fun senderAddressClicked() = launch {
        showExternalActions(senderModel.first().address, originChain)
    }

    private suspend fun showExternalActions(
        address: String,
        chain: Deferred<Chain>,
    ) {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain())
    }

    fun submitClicked() = launch {
        val payload = buildValidationPayload()
        validationExecutor.requireValid(
            validationSystem = nftSendInteractor.defaultValidationSystem(),
            payload = payload,
            progressConsumer = _transferSubmittingFlow.progressConsumer(),
            validationFailureTransformer = { mapNftTransferValidationFailureToUI(resourceManager, it) }
        ) {
            performTransfer(it.transfer)
        }
    }

    private fun setInitialState() = launch {
        originFeeMixin.setFee(transferDraft.originFee)
    }

    private suspend fun createAddressModel(
        address: String,
        chain: Chain,
        resolveName: Boolean
    ) =
        addressIconGenerator.createAddressModel(
            chain = chain,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            address = address,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT,
            addressDisplayUseCase = addressDisplayUseCase.takeIf { resolveName }
        )

    private fun performTransfer(
        transfer: NftTransferModel
    ) = launch {
        nftSendInteractor.performTransfer(transfer)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishSendFlow()
            }.onFailure(::showError)

        _transferSubmittingFlow.value = false
    }

    private fun finishSendFlow() {
        router.closeSendNftFlow()
    }

    private suspend fun buildValidationPayload(): NftTransferPayload {
        val chain = originChain()
        return NftTransferPayload(
            transfer = NftTransferModel(
                sender = currentAccount.first(),
                recipient = transferDraft.recipientAddress,
                nftId = transferDraft.nftId,
                nftType = transferDraft.nftType.mapToDomain(),
                originChain = chain,
                destinationChain = chain
            ),
            originFee = transferDraft.originFee,
            originFeeAsset = commissionAssetFlow.first()
        )
    }
}
