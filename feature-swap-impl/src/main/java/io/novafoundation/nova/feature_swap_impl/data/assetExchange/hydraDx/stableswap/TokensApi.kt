package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

@JvmInline
value class TokensApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.hydraTokens: TokensApi
    get() = TokensApi(tokens())

context(StorageQueryContext)
val TokensApi.totalIssuance: QueryableStorageEntry1<HydraDxAssetId, Balance>
    get() = storage1(
        name = "TotalIssuance",
        binding = { decoded, _ -> bindNumber(decoded) },
    )
