package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal

data class NominationPoolsBondMoreValidationPayload(
    val poolMember: PoolMember,
    val amount: BigDecimal,
    val fee: Fee,
    val asset: Asset,
)
