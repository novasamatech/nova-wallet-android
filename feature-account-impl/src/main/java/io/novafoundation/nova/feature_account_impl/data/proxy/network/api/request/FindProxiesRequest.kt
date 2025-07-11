package io.novafoundation.nova.feature_account_impl.data.proxy.network.api.request

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.toHexWithPrefix
import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters

class FindProxiesRequest(
    accountIds: Collection<AccountIdKey>
) : SubQueryFilters {

    @Transient
    private val accountIdsHex = accountIds.map { it.toHexWithPrefix() }

    val query = """
        query {
          proxieds(
            filter: {
              ${"proxyAccountId" presentIn accountIdsHex},
              ${"delay" equalTo 0}
            }) {
            nodes {
              chainId
              type
              proxyAccountId
              accountId
            }
          }
        }
    """.trimIndent()
}


