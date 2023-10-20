package io.novafoundation.nova.feature_nft_impl.data.source.transfers

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_nft_impl.data.network.blockchain.nfts.transfers.NftTransferModel
import io.novafoundation.nova.feature_nft_impl.data.source.BaseNftTransfer
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvidersRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import javax.inject.Inject

@FeatureScope
class Rmrk1NftTransfer @Inject constructor(
    extrinsicService: ExtrinsicService,
    phishingValidationFactory: PhishingValidationFactory,
    nftProvidersRegistry: NftProvidersRegistry,
    nftDao: NftDao,
    exceptionHandler: HttpExceptionHandler,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
): BaseNftTransfer(
    extrinsicService,
    phishingValidationFactory,
    nftProvidersRegistry,
    nftDao,
    exceptionHandler,
    enoughTotalToStayAboveEDValidationFactory
) {

    override fun ExtrinsicBuilder.transfer(transfer: NftTransferModel) {
        throw UnsupportedOperationException("RmrkV1 doesn't supported")
    }

    override fun areTransfersSupported(chain: Chain): Boolean = false
}
