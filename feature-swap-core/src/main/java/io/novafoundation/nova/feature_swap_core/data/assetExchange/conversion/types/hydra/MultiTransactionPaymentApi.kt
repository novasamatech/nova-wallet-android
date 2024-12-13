@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.multiTransactionPayment
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.math.BigInteger

@JvmInline
value class MultiTransactionPaymentApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.multiTransactionPayment: MultiTransactionPaymentApi
    get() = MultiTransactionPaymentApi(multiTransactionPayment())

context(StorageQueryContext)
val MultiTransactionPaymentApi.acceptedCurrencies: QueryableStorageEntry1<HydraDxAssetId, BigInteger>
    get() = storage1(
        name = "AcceptedCurrencies",
        binding = { decoded, _ -> bindNumber(decoded) },
    )

context(StorageQueryContext)
val MultiTransactionPaymentApi.accountCurrencyMap: QueryableStorageEntry1<AccountId, HydraDxAssetId>
    get() = storage1(
        name = "AccountCurrencyMap",
        binding = { decoded, _ -> bindNumber(decoded) },
    )
