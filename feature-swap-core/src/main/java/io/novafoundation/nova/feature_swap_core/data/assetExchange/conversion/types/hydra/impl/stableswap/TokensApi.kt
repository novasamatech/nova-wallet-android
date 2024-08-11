package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrmlAccountData
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry2
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novafoundation.nova.runtime.storage.source.query.api.storage2
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class TokensApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.hydraTokens: TokensApi
    get() = TokensApi(tokens())

context(StorageQueryContext)
val TokensApi.totalIssuance: QueryableStorageEntry1<HydraDxAssetId, BigInteger>
    get() = storage1(
        name = "TotalIssuance",
        binding = { decoded, _ -> bindNumber(decoded) },
    )

context(StorageQueryContext)
val TokensApi.accounts: QueryableStorageEntry2<AccountId, HydraDxAssetId, AccountBalance>
    get() = storage2(
        name = "Accounts",
        binding = { decoded, _, _ -> bindOrmlAccountData(decoded) },
    )
