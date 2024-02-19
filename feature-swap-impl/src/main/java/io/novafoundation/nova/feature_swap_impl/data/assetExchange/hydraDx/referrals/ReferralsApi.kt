@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.utils.referralsOrNull
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

@JvmInline
value class ReferralsApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.referralsOrNull: ReferralsApi?
    get() = referralsOrNull()?.let(::ReferralsApi)

context(StorageQueryContext)
val ReferralsApi.linkedAccounts: QueryableStorageEntry1<AccountId, AccountId>
    get() = storage1(
        name = "LinkedAccounts",
        binding = { decoded, _ -> bindAccountId(decoded) },
    )
