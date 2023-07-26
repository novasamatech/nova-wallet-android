package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindActiveEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindNominations
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSessionIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindStakingLedger
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class StakingRuntimeApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.staking: StakingRuntimeApi
    get() = StakingRuntimeApi(staking())

context(StorageQueryContext)
val StakingRuntimeApi.ledger: QueryableStorageEntry1<AccountId, StakingLedger>
    get() = storage1("Ledger", binding = { decoded, _ -> bindStakingLedger(decoded) })

context(StorageQueryContext)
val StakingRuntimeApi.nominators: QueryableStorageEntry1<AccountId, Nominations>
    get() = storage1("Nominators", binding = { decoded, _ -> bindNominations(decoded) })

context(StorageQueryContext)
val StakingRuntimeApi.bonded: QueryableStorageEntry1<AccountId, AccountId>
    get() = storage1("Bonded", binding = { decoded, _ -> bindAccountId(decoded) })

context(StorageQueryContext)
val StakingRuntimeApi.activeEra: QueryableStorageEntry0<EraIndex>
    get() = storage0("ActiveEra", binding = ::bindActiveEra)

context(StorageQueryContext)
val StakingRuntimeApi.erasStartSessionIndex: QueryableStorageEntry1<EraIndex, BigInteger>
    get() = storage1("ErasStartSessionIndex", binding = { decoded, _ -> bindSessionIndex(decoded) })
