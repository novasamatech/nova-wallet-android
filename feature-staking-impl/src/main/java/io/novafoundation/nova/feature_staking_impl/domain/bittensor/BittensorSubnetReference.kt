package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import java.math.BigInteger

/** A netuid can be reused after deregistration; the registration block identifies its incarnation. */
data class BittensorSubnetReference(val subnetId: BittensorNetuid.Subnet, val registrationBlock: BigInteger) {

    init {
        require(registrationBlock.isNonNegative && registrationBlock.bitLength() <= 64)
    }
}
