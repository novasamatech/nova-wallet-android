package io.novafoundation.nova.feature_governance_impl.data.preimage

import io.novafoundation.nova.common.utils.kilobytes
import java.math.BigInteger

interface PreImageSizer {

    enum class SizeConstraint {
        SMALL
    }

    fun satisfiesSizeConstraint(preImageSize: BigInteger, constraint: SizeConstraint): Boolean
}

class RealPreImageSizer : PreImageSizer {

    override fun satisfiesSizeConstraint(preImageSize: BigInteger, constraint: PreImageSizer.SizeConstraint): Boolean {
        return preImageSize < constraint.threshold
    }

    private val PreImageSizer.SizeConstraint.threshold: BigInteger
        get() = when (this) {
            PreImageSizer.SizeConstraint.SMALL -> 1.kilobytes
        }
}
