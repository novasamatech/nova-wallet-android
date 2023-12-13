package io.novafoundation.nova.feature_staking_impl.data.network.subquery

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.subquery.EraValidatorInfoQueryResponse
import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.model.stakingExternalApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingValidatorEraInfosRequest
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import java.math.BigInteger

data class PayoutTarget(val validatorStash:  AccountIdKey, val era: BigInteger)

class SubQueryValidatorSetFetcher(
    private val stakingApi: StakingApi,
) {

    suspend fun findNominatorPayoutTargets(
        chain: Chain,
        stashAccountAddress: String,
        eraRange: List<EraIndex>,
    ): List<PayoutTarget> {
        // TODO test code while subQuery westend doesn't work
        return eraRange.map {
            PayoutTarget(
                validatorStash = "5C556QTtg1bJ43GDSgeowa3Ark6aeSHGTac1b2rKSXtgmSmW".toAccountId().intoKey(),
                era = it
            )
        }

//        return findPayoutTargets(chain) {apiUrl ->
//            stakingApi.getNominatorEraInfos(
//                apiUrl,
//                StakingNominatorEraInfosRequest(
//                    eraFrom = eraRange.first(),
//                    eraTo = eraRange.last(),
//                    nominatorStashAddress = stashAccountAddress
//                )
//            )
//        }
    }

    suspend fun findValidatorPayoutTargets(
        chain: Chain,
        stashAccountAddress: String,
        eraRange: List<EraIndex>,
    ): List<PayoutTarget> {
        return findPayoutTargets(chain) {apiUrl ->
            stakingApi.getValidatorEraInfos(
                apiUrl,
                StakingValidatorEraInfosRequest(
                    eraFrom = eraRange.first(),
                    eraTo = eraRange.last(),
                    validatorStashAddress = stashAccountAddress
                )
            )
        }
    }

    private suspend fun findPayoutTargets(
        chain: Chain,
        apiCall: suspend (url: String) -> SubQueryResponse<EraValidatorInfoQueryResponse>
    ): List<PayoutTarget> {
        val stakingExternalApi = chain.stakingExternalApi() ?: return emptyList()

        val validatorsInfos = apiCall(stakingExternalApi.url)

        val nodes = validatorsInfos.data.eraValidatorInfos?.nodes.orEmpty()

        return nodes.map {
            PayoutTarget(it.address.toAccountId().intoKey(), it.era)
        }
    }
}
