package io.novafoundation.nova.feature_staking_impl.data.network.subquery

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger

data class PayoutTarget(val validatorStash: AccountIdKey, val era: BigInteger)
