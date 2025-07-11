package io.novafoundation.nova.feature_account_impl.data.multisig.api.request

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.toHexWithPrefix
import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters

class FindMultisigsRequest(
    accountIds: Set<AccountIdKey>
) : SubQueryFilters {
    @Transient
    private val accountIdsHex = accountIds.map { it.toHexWithPrefix() }

    val query = """
        query {
          accountMultisigs(
            filter: {
              signatory: {
                ${"id" presentIn accountIdsHex}
              }
            }
          ) {
            nodes {
              multisig {
                threshold
                signatories {
                  nodes {
                    signatoryId
                  }
                }
                id
              }
            }
          }
        }
    """.trimIndent()
}
