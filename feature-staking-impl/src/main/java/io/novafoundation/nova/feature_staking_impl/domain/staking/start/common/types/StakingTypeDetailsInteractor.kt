package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger


interface StakingTypeDetailsInteractor {

    fun observeData(): Flow<StakingTypeDetails>

    suspend fun getAvailableBalance(asset: Asset): BigInteger
}
