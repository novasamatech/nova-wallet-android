package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.allOf
import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.anyOf
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingTypeToStakingString
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix

class StakingStatsRequest(metaAccount: MetaAccount, chains: List<Chain>) {

    @Transient
    private val chainAddressesFilter = constructChainAddressesFilter(metaAccount, chains)

    val query = """
    {
        query {
            activeStakers(
                filter: { $chainAddressesFilter }
            ) {
                nodes {
                    networkId
                    stakingType
                    address
                }
            }
            
            stakingApies {
                nodes {
                    networkId
                    stakingType
                    maxAPY
                }
            }
            
            accumulatedRewards(
                filter: { $chainAddressesFilter }
            ) {
                nodes {
                    networkId
                    stakingType
                    amount
                }
            }
        }
    }

    """.trimIndent()

    private fun constructChainAddressesFilter(metaAccount: MetaAccount, chains: List<Chain>): String = with(SubQueryFilters) {
        val perChain = chains.mapNotNull { chain ->
            val address = metaAccount.addressIn(chain) ?: return@mapNotNull null

            val hasStakingTypeList = chain.utilityAsset.supportedStakingOptions().mapNotNull { stakingType ->
                mapStakingTypeToStakingString(stakingType)?.let { hasStakingType(it) }
            }

            allOf(
                hasAddress(address),
                hasNetwork(chain.id.requireHexPrefix()),
                anyOf(hasStakingTypeList)
            )
        }

        return anyOf(perChain)
    }

    private fun SubQueryFilters.hasNetwork(chainId: ChainId): String {
        return "networkId" equalTo chainId
    }

    private fun SubQueryFilters.hasStakingType(stakingType: String): String {
        return "stakingType" equalTo stakingType
    }

    private fun SubQueryFilters.hasAddress(address: String): String {
        return "address" equalTo address
    }
}
