package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.model.BondedEras
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.SessionIndex
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.UnappliedSlashKey
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bind
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindActiveEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindEraIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindNominations
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSessionIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindStakingLedger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindValidatorPrefs
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry2
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novafoundation.nova.runtime.storage.source.query.api.storage0OrNull
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1OrNull
import io.novafoundation.nova.runtime.storage.source.query.api.storage2
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class StakingRuntimeApi(override val module: Module) : QueryableModule

context(RuntimeContext)
val RuntimeMetadata.staking: StakingRuntimeApi
    get() = StakingRuntimeApi(staking())

context(RuntimeContext)
val StakingRuntimeApi.ledger: QueryableStorageEntry1<AccountId, StakingLedger>
    get() = storage1("Ledger", binding = { decoded, _ -> bindStakingLedger(decoded) })

context(RuntimeContext)
val StakingRuntimeApi.nominators: QueryableStorageEntry1<AccountId, Nominations>
    get() = storage1("Nominators", binding = { decoded, _ -> bindNominations(decoded) })

context(RuntimeContext)
val StakingRuntimeApi.validators: QueryableStorageEntry1<AccountId, ValidatorPrefs>
    get() = storage1("Validators", binding = { decoded, _ -> bindValidatorPrefs(decoded) })

context(RuntimeContext)
val StakingRuntimeApi.bonded: QueryableStorageEntry1<AccountId, AccountId>
    get() = storage1("Bonded", binding = { decoded, _ -> bindAccountId(decoded) })

context(RuntimeContext)
val StakingRuntimeApi.activeEra: QueryableStorageEntry0<EraIndex>
    get() = storage0("ActiveEra", binding = ::bindActiveEra)

context(RuntimeContext)
val StakingRuntimeApi.erasStartSessionIndexOrNull: QueryableStorageEntry1<EraIndex, SessionIndex>?
    get() = storage1OrNull("ErasStartSessionIndex", binding = { decoded, _ -> bindSessionIndex(decoded) })

context(RuntimeContext)
val StakingRuntimeApi.bondedErasOrNull: QueryableStorageEntry0<BondedEras>?
    get() = storage0OrNull("BondedEras", binding = BondedEras.Companion::bind)

context(RuntimeContext)
val StakingRuntimeApi.unappliedSlashes: QueryableStorageEntry2<EraIndex, UnappliedSlashKey, Unit>
    get() = storage2(
        name = "UnappliedSlashes",
        binding = { _, _, _ -> },
        key1ToInternalConverter = { it },
        key2ToInternalConverter = { TODO("Not yet needed") },
        key1FromInternalConverter = ::bindEraIndex,
        key2FromInternalConverter = UnappliedSlashKey.Companion::bind
    )
