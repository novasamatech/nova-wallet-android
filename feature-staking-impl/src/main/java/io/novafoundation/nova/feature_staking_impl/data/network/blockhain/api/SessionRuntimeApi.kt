package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api

import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.session
import io.novafoundation.nova.feature_staking_api.domain.model.SessionIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSessionIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindSessionValidators
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry0
import io.novafoundation.nova.runtime.storage.source.query.api.storage0
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class SessionRuntimeApi(override val module: Module) : QueryableModule

context(RuntimeContext)
val RuntimeMetadata.session: SessionRuntimeApi
    get() = SessionRuntimeApi(session())

context(RuntimeContext)
val SessionRuntimeApi.currentIndex: QueryableStorageEntry0<SessionIndex>
    get() = storage0("CurrentIndex", binding = ::bindSessionIndex)

context(RuntimeContext)
val SessionRuntimeApi.validators: QueryableStorageEntry0<SessionValidators>
    get() = storage0("Validators", binding = ::bindSessionValidators)
