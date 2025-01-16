package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api

import io.novafoundation.nova.common.utils.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.bindUserStakeInfo
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module


@JvmInline
value class CollatorStakingRuntimeApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.collatorStaking: CollatorStakingRuntimeApi
    get() = CollatorStakingRuntimeApi(collatorStaking())


context(StorageQueryContext)
val CollatorStakingRuntimeApi.userStake: QueryableStorageEntry1<AccountId, UserStakeInfo>
    get() = storage1("UserStake", binding = { decoded, _ -> bindUserStakeInfo(decoded) })
