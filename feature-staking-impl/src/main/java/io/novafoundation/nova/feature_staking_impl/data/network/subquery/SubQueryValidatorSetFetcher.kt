package io.novafoundation.nova.feature_staking_impl.data.network.subquery

import io.novafoundation.nova.common.data.network.subquery.EraValidatorInfoQueryResponse.EraValidatorInfo.Nodes.Node
import io.novafoundation.nova.common.utils.networkType
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.historicalEras
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingEraValidatorInfosRequest
import io.novafoundation.nova.feature_staking_impl.data.repository.subqueryFearlessApiPath
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class SubQueryValidatorSetFetcher(
    private val stakingApi: StakingApi,
    private val stakingRepository: StakingRepository,
) {

    suspend fun fetchAllValidators(chainId: ChainId, stashAccountAddress: String): List<String> {
        val historicalRange = stakingRepository.historicalEras(chainId)
        val subqueryPath = stashAccountAddress.networkType().subqueryFearlessApiPath()

        val validatorsInfos = stakingApi.getValidatorsInfo(
            subqueryPath,
            StakingEraValidatorInfosRequest(
                eraFrom = historicalRange.first(),
                eraTo = historicalRange.last(),
                accountAddress = stashAccountAddress
            )
        )

        return validatorsInfos.data.query?.eraValidatorInfos?.nodes?.map(
            Node::address
        )?.distinct().orEmpty()
    }
}
