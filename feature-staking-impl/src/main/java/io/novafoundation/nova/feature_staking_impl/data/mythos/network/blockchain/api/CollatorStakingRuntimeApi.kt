package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.bindUserStakeInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class CollatorStakingRuntimeApi(override val module: Module) : QueryableModule

context(RuntimeContext)
val RuntimeMetadata.collatorStaking: CollatorStakingRuntimeApi
    get() = CollatorStakingRuntimeApi(collatorStaking())

context(RuntimeContext)
val CollatorStakingRuntimeApi.userStake: QueryableStorageEntry1<AccountId, UserStakeInfo>
    get() = storage1("UserStake", binding = { decoded, _ -> bindUserStakeInfo(decoded) })

context(RuntimeContext)
val CollatorStakingRuntimeApi.minStake: QueryableStorageEntry0<Balance>
    get() = storage0("MinStake", binding = ::bindNumber)
