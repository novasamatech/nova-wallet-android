package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

import java.math.BigInteger

class StakingNominatorEraInfosRequest(eraFrom: BigInteger, eraTo: BigInteger, nominatorStashAddress: String) {
    val query = """
            query {
                eraValidatorInfos(
                    filter:{
                        era:{ greaterThanOrEqualTo: $eraFrom, lessThanOrEqualTo: $eraTo},
                        others:{ contains:[{who: "$nominatorStashAddress"}]}
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
