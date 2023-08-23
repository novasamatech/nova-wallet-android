package io.novafoundation.nova.feature_nft_impl.data.source.transfers

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferModel
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.statemineNftTransfer
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.statemineUniqueTransfer
import io.novafoundation.nova.feature_nft_impl.data.source.NftTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import javax.inject.Inject

class NftsNftTransfer @Inject constructor(
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicService: ExtrinsicService,
    phishingValidationFactory: PhishingValidationFactory,
    nftRepository: NftRepository
): NftTransfer(assetSourceRegistry, extrinsicService, phishingValidationFactory, nftRepository) {

    override fun ExtrinsicBuilder.transfer(transfer: NftTransferModel) {
        require(transfer.nftType is Nft.Type.Nfts)

        statemineNftTransfer(
            nftType = transfer.nftType,
            target = transfer.originChain.accountIdOrDefault(transfer.recipient)
        )
    }
}
