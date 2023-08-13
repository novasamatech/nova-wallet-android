package io.novafoundation.nova.feature_assets.presentation.send.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressForTransactionRequester
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferModel
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferPayload
import io.novafoundation.nova.feature_nft_impl.domain.common.mapNftCollectionForUi
import io.novafoundation.nova.feature_nft_impl.domain.nft.details.NftDetailsInteractor
import io.novafoundation.nova.feature_nft_impl.domain.nft.send.NftSendInteractor
import io.novafoundation.nova.feature_nft_impl.presentation.NftPayload
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.NftTransferDraft
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.mapNftTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigInteger

class InputAddressNftViewModel(
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val router: NftRouter,
    val nftPayload: NftPayload,
    private val nftDetailsInteractor: NftDetailsInteractor,
    private val nftSendInteractor: NftSendInteractor,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressInputMixinFactory: AddressInputMixinFactory,
    private val selectAddressRequester: SelectAddressForTransactionRequester,
    private val externalActions: ExternalActions.Presentation,
    private val resourceManager: ResourceManager
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val nftDetailsFlow = nftDetailsInteractor.nftDetailsFlow(nftPayload.identifier)
        .inBackground()
        .share()

    val nftName = nftDetailsFlow.map {
        it.nftDetails.name
    }
    val nftCollectionName = nftDetailsFlow.map {
        val collection = it.nftDetails.collection
        mapNftCollectionForUi(collection?.name, collection?.id)
    }

    private val chainFlow = nftDetailsFlow
        .map { it.nftDetails.chain }
        .inBackground()
        .share()

    val addressInputMixin = with(addressInputMixinFactory) {
        create(
            inputSpecProvider = singleChainInputSpec(chainFlow),
            errorDisplayer = this@InputAddressNftViewModel::showError,
            showAccountEvent = this@InputAddressNftViewModel::showAccountDetails,
            coroutineScope = viewModelScope
        )
    }

    val isSelectAddressAvailable = flow {
        emit(
            metaAccountGroupingInteractor.hasAvailableMetaAccountsForDestination(
                nftPayload.chainId,
                nftPayload.chainId
            )
        )
    }
        .inBackground()
        .share()

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    private val commissionAssetFlow = nftSendInteractor.commissionAssetFlow(nftPayload.chainId)
        .inBackground()
        .share()

    val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow.map { it.token })

    init {
        subscribeOnSelectAddress()
        setupFees()
    }

    fun nextClicked() {
        originFeeMixin.requireFee(this) { originFee ->
            launch {
                val nftDetails = nftDetailsFlow.first().nftDetails
                val chain = chainFlow.first()
                val nftTransferPayload = NftTransferPayload(
                    transfer = NftTransferModel(
                        sender = selectedAccount.first(),
                        recipient = addressInputMixin.getAddress(),
                        nftId = nftDetails.identifier,
                        nftType = nftDetails.type,
                        originChain = chain,
                        destinationChain = chain
                    ),
                    originFee = originFee,
                    originFeeAsset = commissionAssetFlow.first()
                )
                validationExecutor.requireValid(
                    validationSystem = nftSendInteractor.defaultValidationSystem(),
                    payload = nftTransferPayload,
                    validationFailureTransformer = { mapNftTransferValidationFailureToUI(resourceManager, it) }
                ) {
                    openConfirmScreen(it)
                }
            }
        }
    }

    fun backClicked() {
        router.back()
    }

    fun selectRecipientWallet() {
        launch {
            val selectedAddress = addressInputMixin.inputFlow.value
            val request = SelectAddressForTransactionRequester.Request(nftPayload.chainId, nftPayload.chainId, selectedAddress)
            selectAddressRequester.openRequest(request)
        }
    }

    private fun showAccountDetails(address: String) {
        launch {
            val chain = chainFlow.first()
            externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
        }
    }

    private fun subscribeOnSelectAddress() {
        selectAddressRequester.responseFlow
            .onEach {
                addressInputMixin.inputFlow.value = it.selectedAddress
            }
            .launchIn(this)
    }

    private fun setupFees() {
        originFeeMixin.setupFee { transfer -> nftSendInteractor.getOriginFee(transfer) }
    }

    private fun FeeLoaderMixin.Presentation.setupFee(
        feeConstructor: suspend (transfer: NftTransferModel) -> BigInteger
    ) {
        connectWith(
            inputSource = addressInputMixin.inputFlow,
            scope = viewModelScope,
            feeConstructor = { addressInput ->
                val transfer = buildTransfer(addressInput)

                feeConstructor(transfer)
            }
        )
    }

    private fun openConfirmScreen(validPayload: NftTransferPayload) = launch {
        val nftDetails = nftDetailsFlow.first().nftDetails
        val transferDraft = NftTransferDraft(
            originFee = validPayload.originFee,
            nftId = nftDetails.identifier,
            nftType = nftDetails.type,
            recipientAddress = validPayload.transfer.recipient,
            chainId = chainFlow.first().id,
            name = nftDetails.name,
            collectionName = nftCollectionName.first()
        )
        router.openConfirmScreen(transferDraft)
    }

    private suspend fun buildTransfer(address: String): NftTransferModel {
        val nftDetails = nftDetailsFlow.first().nftDetails
        val chain = chainFlow.first()
        return NftTransferModel(
            sender = selectedAccount.first(),
            recipient = address,
            originChain = chain,
            nftId = nftDetails.identifier,
            nftType = nftDetails.type,
            destinationChain = chain
        )
    }
}
