package io.novafoundation.nova.feature_nft_impl.presentation.nft.details

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.domain.nft.details.NftDetailsInteractor
import io.novafoundation.nova.feature_nft_impl.domain.nft.details.PricedNftDetails
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.formatIssuance
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NftDetailsViewModel(
    private val router: NftRouter,
    private val resourceManager: ResourceManager,
    private val interactor: NftDetailsInteractor,
    private val nftIdentifier: String,
    private val externalActionsDelegate: ExternalActions.Presentation,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase
) : BaseViewModel(), ExternalActions by externalActionsDelegate {

    private val nftDetailsFlow = interactor.nftDetailsFlow(nftIdentifier)
        .inBackground()
        .share()

    val nftDetailsUi = nftDetailsFlow
        .map(::mapNftDetailsToUi)
        .inBackground()
        .share()

    fun ownerClicked() = launch {
        val pricedNftDetails = nftDetailsFlow.first()

        with(pricedNftDetails.nftDetails) {
            externalActionsDelegate.showAddressActions(owner, chain)
        }
    }

    fun creatorClicked() = launch {
        val pricedNftDetails = nftDetailsFlow.first()

        with(pricedNftDetails.nftDetails) {
            externalActionsDelegate.showAddressActions(creator!!, chain)
        }
    }

    private suspend fun mapNftDetailsToUi(pricedNftDetails: PricedNftDetails): NftDetailsModel {
        val nftDetails = pricedNftDetails.nftDetails

        return NftDetailsModel(
            media = nftDetails.media,
            name = nftDetails.name,
            issuance = resourceManager.formatIssuance(nftDetails.issuance),
            description = nftDetails.description,
            price = pricedNftDetails.price?.let {
                mapAmountToAmountModel(it.amount, it.token)
            },
            collection = nftDetails.collection?.let {
                NftDetailsModel.Collection(
                    name = it.name ?: it.id,
                    media = it.media,
                )
            },
            owner = addressIconGenerator.createAddressModel(
                chain = nftDetails.chain,
                accountId = nftDetails.owner,
                sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                addressDisplayUseCase = addressDisplayUseCase
            ),
            creator = nftDetails.creator?.let {
                addressIconGenerator.createAddressModel(
                    chain = nftDetails.chain,
                    accountId = it,
                    sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                    addressDisplayUseCase = addressDisplayUseCase
                )
            },
            network = mapChainToUi(nftDetails.chain)
        )
    }

    fun backClicked() {
        router.back()
    }
}
