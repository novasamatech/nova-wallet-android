package io.novafoundation.nova.feature_account_impl.data.multisig.api.request

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.toHexWithPrefix
import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash

class OffChainPendingMultisigInfoRequest(
    accountIdKey: AccountIdKey,
    callHashes: Collection<CallHash>
) : SubQueryFilters {

    @Transient
    private val callHashesHex = callHashes.map { it.toHexWithPrefix() }

    val query = """
        query {
          multisigOperations(filter:  {
             ${"accountId" equalTo accountIdKey.toHexWithPrefix() }
             ${"status" equalToEnum "pending"}
             ${"callHash" presentIn callHashesHex}
          }) {
            nodes {
              callHash
              callData
              timestamp
              events(last: 1) {
                nodes {
                  timestamp
                }
              }
            }
          }
        }
    """.trimIndent()
}
