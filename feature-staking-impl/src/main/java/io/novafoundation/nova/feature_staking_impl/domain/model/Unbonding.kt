package io.novafoundation.nova.feature_staking_impl.domain.model

import java.math.BigInteger

class Unbonding(val amount: BigInteger, val timeLeft: Long, val calculatedAt: Long)
