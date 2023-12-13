package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters
import java.math.BigInteger

class StakingValidatorEraInfosRequest(eraFrom: BigInteger, eraTo: BigInteger, validatorStashAddress: String): SubQueryFilters {
    val query = """
            query {
                eraValidatorInfos(
                    filter:{
                        era:{ greaterThanOrEqualTo: $eraFrom, lessThanOrEqualTo: $eraTo},
                        ${"address" equalTo validatorStashAddress}
                    }
                ) {
                    nodes {
                        id
                        address
                        era
                        total
                        own
                    }
                }
            }
    """.trimIndent()
}
