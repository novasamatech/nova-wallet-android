@file:Suppress("RedundantUnitExpression")

package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.multiTransactionPayment
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableModule
import io.novafoundation.nova.runtime.storage.source.query.api.QueryableStorageEntry1
import io.novafoundation.nova.runtime.storage.source.query.api.storage1
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

@JvmInline
value class MultiTransactionPaymentApi(override val module: Module) : QueryableModule

context(StorageQueryContext)
val RuntimeMetadata.multiTransactionPayment: MultiTransactionPaymentApi
    get() = MultiTransactionPaymentApi(multiTransactionPayment())

context(StorageQueryContext)
val MultiTransactionPaymentApi.acceptedCurrencies: QueryableStorageEntry1<HydraDxAssetId, Balance>
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
