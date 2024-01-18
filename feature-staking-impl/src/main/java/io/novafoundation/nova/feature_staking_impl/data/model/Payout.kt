package io.novafoundation.nova.feature_staking_impl.data.model

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger

class Payout(
    val validatorStash: AccountIdKey,
    val era: BigInteger,
    val amount: BigInteger,
    val pagesToClaim: List<Int>
)
