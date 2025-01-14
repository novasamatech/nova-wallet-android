package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api

import io.novafoundation.nova.common.utils.collatorStaking
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.StakingRuntimeApi
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindStakingLedger
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
val RuntimeMetadata.collatorStaking: StakingRuntimeApi
    get() = StakingRuntimeApi(collatorStaking())


context(StorageQueryContext)
val StakingRuntimeApi.ledger: QueryableStorageEntry1<AccountId, StakingLedger>
    get() = storage1("Ledger", binding = { decoded, _ -> bindStakingLedger(decoded) })
