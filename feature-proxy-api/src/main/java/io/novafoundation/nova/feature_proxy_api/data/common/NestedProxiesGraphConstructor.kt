package io.novafoundation.nova.feature_proxy_api.data.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_proxy_api.data.common.NestedProxiesGraphConstructor.Node
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_api.domain.model.min
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface NestedProxiesGraphConstructor {

    fun build(): List<Node>

    data class Node(
        val accountId: AccountIdKey,
        val permissionType: ProxyType,
        val nestedNodes: MutableList<Node>,
        val path: Map<AccountIdKey, ProxyType>
    ) {
        fun hasInPath(otherAccountId: AccountIdKey): Boolean {
            return path.contains(otherAccountId) || accountId == otherAccountId
        }

        fun addNested(node: Node) {
            nestedNodes.add(node)
        }

        fun flatten(): List<Node> {
            val result = mutableListOf<Node>()
            result.add(this)
            result.addAll(nestedNodes.flatMap { it.flatten() })
            return result
        }

        companion object
    }
}

fun Node.Companion.getAllAccountIds(nodes: List<Node>): Set<AccountId> {
    return flatten(nodes).mapToSet { it.accountId.value }
}

fun Node.Companion.flatten(nodes: List<Node>): List<Node> {
    return nodes.flatMap { it.flatten() }
}

fun ProxyType.isMutuallyExclusiveWith(node: Node): Boolean {
    val fullPermissionPath = node.path.values + node.permissionType
    return fullPermissionPath.any { ProxyType.min(it, this) == null }
}
