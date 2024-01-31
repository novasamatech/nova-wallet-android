package io.novafoundation.nova.feature_proxy_impl.data.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_proxy_api.data.common.NestedProxiesGraphConstructor
import io.novafoundation.nova.feature_proxy_api.data.common.NestedProxiesGraphConstructor.Node
import io.novafoundation.nova.feature_proxy_api.data.common.isMutuallyExclusiveWith
import io.novafoundation.nova.feature_proxy_api.data.model.ProxyPermission
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType

class RealNestedProxiesGraphConstructor(
    val startAccountIds: Set<AccountIdKey>,
    permissions: List<ProxyPermission>
) : NestedProxiesGraphConstructor {

    val proxyToProxieds: Map<AccountIdKey, List<Node>> = permissions
        .groupBy { it.proxyAccountId }
        .mapValues {
            it.value.map {
                Node(
                    it.proxiedAccountId,
                    it.proxyType,
                    mutableListOf(),
                    mapOf()
                )
            }
        }

    override fun build(): List<Node> {
        val startNodes = startAccountIds.map { Node(it, ProxyType.Any, mutableListOf(), emptyMap()) }

        fillNodes(startNodes)

        return startNodes
    }

    fun fillNodes(nodes: List<Node>) {
        for (node in nodes) {
            val onChainNestedNodes = proxyToProxieds[node.accountId] ?: continue

            val resultNestedNodes = mutableListOf<Node>()

            for (proxiedNode in onChainNestedNodes) {
                // If we have an account in full node path we skip it to avoid cycles
                if (node.hasInPath(proxiedNode.accountId)) continue

                // Check that proxy type is not matually exclusive by full path
                val matuallyExclusive = proxiedNode.permissionType.isMutuallyExclusiveWith(node)
                if (matuallyExclusive) continue

                val nestedNodePath = node.path + mapOf(node.accountId to node.permissionType)
                val nestedNode = proxiedNode.copy(path = nestedNodePath)
                resultNestedNodes.add(nestedNode)
            }

            if (resultNestedNodes.isNotEmpty()) {
                node.setNested(resultNestedNodes)
                fillNodes(node.nestedNodes)
            }
        }
    }
}
