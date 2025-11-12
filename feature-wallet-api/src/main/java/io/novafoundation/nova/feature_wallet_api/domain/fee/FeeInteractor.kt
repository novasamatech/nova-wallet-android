package io.novafoundation.nova.feature_wallet_api.domain.fee

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface FeeInteractor {

    suspend fun canPayFeeInAsset(chainAsset: Chain.Asset): Boolean

    suspend fun assetFlow(asset: Chain.Asset): Flow<Asset?>

    suspend fun hasEnoughBalanceToPayFee(feeAsset: Asset, inspectedFeeAmount: FeeInspector.InspectedFeeAmount): Boolean

    suspend fun getToken(chainAsset: Chain.Asset): Token
}
