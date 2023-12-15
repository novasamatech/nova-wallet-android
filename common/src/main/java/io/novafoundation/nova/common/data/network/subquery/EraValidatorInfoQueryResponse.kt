package io.novafoundation.nova.common.data.network.subquery

import java.math.BigInteger

class EraValidatorInfoQueryResponse(val eraValidatorInfos: SubQueryNodes<EraValidatorInfo>?) {
    class EraValidatorInfo(
        val id: String,
        val address: String,
        val era: BigInteger,
        val total: String,
        val own: String,
    )
}
