package io.novafoundation.nova.feature_account_impl.data.multisig.blockhain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.multisig
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model.OnChainMultisig
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry2
import io.novafoundation.nova.runtime.storage.source.query.api.converters.scaleDecoder
import io.novafoundation.nova.runtime.storage.source.query.api.converters.scaleEncoder
import io.novafoundation.nova.runtime.storage.source.query.api.storage2
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class MultisigRuntimeApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.multisig: MultisigRuntimeApi
    get() = MultisigRuntimeApi(multisig())

context(StorageQueryContext)
val MultisigRuntimeApi.multisigs: QueryableStorageEntry2<AccountIdKey, CallHash, OnChainMultisig>
    get() = storage2(
        name = "Multisigs",
        binding = { decoded, _, callHash -> OnChainMultisig.bind(decoded, callHash) },
        key1ToInternalConverter = AccountIdKey.scaleEncoder,
        key1FromInternalConverter = AccountIdKey.scaleDecoder,
        key2ToInternalConverter = CallHash.scaleEncoder,
        key2FromInternalConverter = CallHash.scaleDecoder
    )
