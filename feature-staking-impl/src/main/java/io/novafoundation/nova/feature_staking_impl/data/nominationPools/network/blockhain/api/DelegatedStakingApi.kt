package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api

import io.novafoundation.nova.common.utils.delegatedStaking
import io.novafoundation.nova.common.utils.delegatedStakingOrNull
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.DelegatedStakingDelegation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.bindDelegatedStakingDelegation
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class DelegatedStakingApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.delegatedStaking: DelegatedStakingApi
    get() = DelegatedStakingApi(delegatedStaking())

context(StorageQueryContext)
val RuntimeMetadata.delegatedStakingOrNull: DelegatedStakingApi?
    get() = delegatedStakingOrNull()?.let(::DelegatedStakingApi)

context(StorageQueryContext)
val DelegatedStakingApi.delegators: QueryableStorageEntry1<AccountId, DelegatedStakingDelegation>
    get() = storage1("Delegators", binding = { decoded, _ -> bindDelegatedStakingDelegation(decoded) })
