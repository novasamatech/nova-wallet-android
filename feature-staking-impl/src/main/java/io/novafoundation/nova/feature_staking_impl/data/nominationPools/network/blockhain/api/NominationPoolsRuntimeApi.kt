package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api

import io.novafoundation.nova.common.utils.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolIdRaw
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.bindBondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.bindPoolMember
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

@JvmInline
value class NominationPoolsApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.nominationPools: NominationPoolsApi
    get() = NominationPoolsApi(nominationPools())


context(StorageQueryContext)
val NominationPoolsApi.bondedPools: QueryableStorageEntry1<PoolIdRaw, BondedPool>
    get() = storage1("BondedPools", binding = { decoded, poolIdRaw -> bindBondedPool(decoded, PoolId(poolIdRaw)) })

context(StorageQueryContext)
val NominationPoolsApi.poolMembers: QueryableStorageEntry1<AccountId, PoolMember>
    get() = storage1("PoolMembers", binding = { decoded, accountId -> bindPoolMember(decoded, accountId) })
