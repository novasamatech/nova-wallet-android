package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface NftProvider {

    suspend fun initialNftsSync(
        chain: Chain,
        metaAccount: MetaAccount,
        forceOverwrite: Boolean,
        at: BlockHash?
    )

    suspend fun subscribeNftOwnerAccountId(
        subscriptionBuilder: StorageSharedRequestsBuilder,
        nftLocal: NftLocal
    ): Flow<AccountId?>

    suspend fun nftFullSync(nft: Nft)

    fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails>
}
