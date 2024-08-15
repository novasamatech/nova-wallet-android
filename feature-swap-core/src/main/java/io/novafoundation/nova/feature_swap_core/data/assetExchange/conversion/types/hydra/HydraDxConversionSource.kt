package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_core.domain.model.SwapQuoteException
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

interface HydraDxConversionSource : Identifiable {

    suspend fun sync()

    suspend fun availableSwapDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection>

    @Throws(SwapQuoteException::class)
    suspend fun quote(args: HydraDxConversionSourceQuoteArgs): BigInteger

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>

    interface Factory {

        fun create(chain: Chain): HydraDxConversionSource
    }
}

data class HydraDxConversionSourceQuoteArgs(
    val chainAssetIn: Chain.Asset,
    val chainAssetOut: Chain.Asset,
    val amount: BigInteger,
    val swapDirection: SwapDirection,
    val params: Map<String, String>
)
