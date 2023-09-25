package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

data class NominationPoolsUnbondValidationPayload(
    val poolMember: PoolMember,
    val stakedBalance: BigDecimal,
    val amount: BigDecimal,
    val fee: BigDecimal,
    val asset: Asset,
    val sharedComputationScope: CoroutineScope,
)
