package io.novafoundation.nova.feature_account_impl.data.multisig.api.request

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.toHexWithPrefix
import io.novafoundation.nova.common.data.network.subquery.SubQueryFilters
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix

class OffChainPendingMultisigInfoRequest(
    accountIdKey: AccountIdKey,
    callHashes: Collection<CallHash>,
    chainId: ChainId
) : SubQueryFilters {

    @Transient
    private val callHashesHex = callHashes.map { it.toHexWithPrefix() }

    val query = """
        query {
          multisigOperations(filter:  {
             ${"accountId" equalTo accountIdKey.toHexWithPrefix() }
             ${"status" equalToEnum "pending"}
             ${"callHash" presentIn callHashesHex}
             ${"chainId" equalTo chainId.requireHexPrefix()}
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
