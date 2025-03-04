package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility

import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BlockchainLock
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.bindBalanceFreezes
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.bindBalanceLocks
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class BalancesRuntimeApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.balances: BalancesRuntimeApi
    get() = BalancesRuntimeApi(balances())

context(StorageQueryContext)
val BalancesRuntimeApi.locks: QueryableStorageEntry1<AccountId, List<BlockchainLock>>
    get() = storage1("Locks", binding = { decoded, _ -> bindBalanceLocks(decoded) })

context(StorageQueryContext)
val BalancesRuntimeApi.freezes: QueryableStorageEntry1<AccountId, List<BlockchainLock>>
    get() = storage1("Freezes", binding = { decoded, _ -> bindBalanceFreezes(decoded) })
