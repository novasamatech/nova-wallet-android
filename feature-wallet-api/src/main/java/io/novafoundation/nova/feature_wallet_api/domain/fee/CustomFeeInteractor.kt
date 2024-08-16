package io.novafoundation.nova.feature_wallet_api.domain.fee

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface CustomFeeInteractor {

    suspend fun canPayFeeInNonUtilityAsset(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): Boolean

    suspend fun assetFlow(asset: Chain.Asset): Flow<Asset>

    suspend fun hasEnoughBalanceToPayFee(commissionAsset: Asset, feeAmount: BigInteger): Boolean
}
