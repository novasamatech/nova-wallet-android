package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface NftProvider {

    suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount)
}
