package io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.network

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry3
import io.novafoundation.nova.runtime.storage.source.query.api.storage3
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class AhOpsApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.ahOps: AhOpsApi
    get() = AhOpsApi(module("AhOps"))

context(StorageQueryContext)
val AhOpsApi.rcLeaseReserve: QueryableStorageEntry3<BlockNumber, ParaId, AccountIdKey, Any>
    get() = storage3(
        name = "RcLeaseReserve",
        binding = { _, _, _, decoded -> decoded },
        key3ToInternalConverter = { it.value },
        key3FromInternalConverter = ::bindAccountIdKey
    )

context(StorageQueryContext)
val AhOpsApi.rcCrowdloanContribution: QueryableStorageEntry3<BlockNumber, ParaId, AccountIdKey, Balance>
    get() = storage3(
        name = "RcCrowdloanContribution",
        binding = { decoded, _, _, _ -> bindContribution(decoded) },
        key3ToInternalConverter = { it.value },
        key3FromInternalConverter = ::bindAccountIdKey
    )

private fun bindContribution(decoded: Any?): Balance {
    val (_, balance) = decoded.castToList() // Tuple

    return bindNumber(balance)
}
