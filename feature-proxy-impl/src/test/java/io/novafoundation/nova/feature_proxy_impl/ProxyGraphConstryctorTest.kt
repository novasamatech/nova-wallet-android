package io.novafoundation.nova.feature_proxy_impl

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_proxy_api.data.common.NestedProxiesGraphConstructor.Node
import io.novafoundation.nova.feature_proxy_api.data.model.ProxyPermission
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_impl.data.common.RealNestedProxiesGraphConstructor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProxyGraphConstryctorTest {

    @Test
    fun test_result_size() {
        val engine = RealNestedProxiesGraphConstructor(
            startAccountIds = keysOf("Account_1", "Account_2"),
            permissions = listOf(makePermission(from = "Account_4", to = "Account_1", ProxyType.Any))
        )

        val result = engine.build()

        assertEquals(result.size, 2)
    }

    @Test
    fun simple_nested_proxies() {
        val engine = RealNestedProxiesGraphConstructor(
            startAccountIds = keysOf("Account_1", "Account_2"),
            permissions = listOf(
                makePermission(from = "Account_4", to = "Account_3", ProxyType.Any),
                makePermission(from = "Account_3", to = "Account_1", ProxyType.Any),
            )
        )

        val result = engine.build()

        assertEquals(
            result,
            listOf(
                makeNode(
                    accountId = "Account_1",
                    nestedNodes = makeSingleNode(
                        accountId = "Account_3",
                        nestedNodes = makeSingleNode(
                            "Account_4",
                            path = pathWithAny("Account_1", "Account_3")
                        ),
                        path = pathWithAny("Account_1")
                    )
                ),
                makeNode("Account_2")
            )
        )
    }

    // Set a timeout to check that we don't have an infinite loop
    @Test(timeout = 10000L)
    fun cyclical_nested_proxies() {
        val engine = RealNestedProxiesGraphConstructor(
            startAccountIds = keysOf("Account_1"),
            permissions = listOf(
                makePermission(from = "Account_3", to = "Account_2", ProxyType.Any),
                makePermission(from = "Account_2", to = "Account_1", ProxyType.Any),
                makePermission(from = "Account_1", to = "Account_3", ProxyType.Any),
            )
        )

        val result = engine.build()

        assertEquals(
            result,
            makeSingleNode(
                accountId = "Account_1",
                nestedNodes = makeSingleNode(
                    accountId = "Account_2",
                    nestedNodes = makeSingleNode(
                        accountId = "Account_3",
                        path = pathWithAny("Account_1", "Account_2")
                    ),
                    path = pathWithAny("Account_1")
                )
            )
        )
    }

    @Test
    fun mutually_exclusive_path_doesnt_build() {
        val engine = RealNestedProxiesGraphConstructor(
            startAccountIds = keysOf("Account_1"),
            permissions = listOf(
                // Staking is not controllable from Governance and should be skipped
                makePermission(from = "Account_4", to = "Account_3", ProxyType.Staking),
                makePermission(from = "Account_3", to = "Account_2", ProxyType.Any),
                makePermission(from = "Account_2", to = "Account_1", ProxyType.Governance)
            )
        )

        val result = engine.build()

        assertEquals(
            result,
            makeSingleNode(
                accountId = "Account_1",
                nestedNodes = makeSingleNode(
                    accountId = "Account_2",
                    permissionType = ProxyType.Governance,
                    nestedNodes = makeSingleNode(
                        accountId = "Account_3",
                        path = path("Account_1" to ProxyType.Any, "Account_2" to ProxyType.Governance)
                    ),
                    path = pathWithAny("Account_1")
                )
            )
        )
    }

    @Test
    fun two_accounts_in_permission_chain() {
        val engine = RealNestedProxiesGraphConstructor(
            startAccountIds = keysOf("Account_1", "Account_2"),
            permissions = listOf(
                makePermission(from = "Account_3", to = "Account_2", ProxyType.Any),
                makePermission(from = "Account_2", to = "Account_1", ProxyType.Any)
            )
        )

        val result = engine.build()

        assertEquals(
            result,
            listOf(
                makeNode(
                    accountId = "Account_1",
                    nestedNodes = makeSingleNode(
                        accountId = "Account_2",
                        nestedNodes = makeSingleNode(
                            accountId = "Account_3",
                            path = pathWithAny("Account_1", "Account_2")
                        ),
                        path = pathWithAny("Account_1")
                    )
                ),
                makeNode(
                    accountId = "Account_2",
                    nestedNodes = makeSingleNode(
                        accountId = "Account_3",
                        path = pathWithAny("Account_2")
                    )
                )
            )
        )
    }


    @Test
    fun cyclical_with_two_accounts_in_permission_chain() {
        val engine = RealNestedProxiesGraphConstructor(
            startAccountIds = keysOf("Account_1", "Account_2"),
            permissions = listOf(
                makePermission(from = "Account_3", to = "Account_2", ProxyType.Any),
                makePermission(from = "Account_2", to = "Account_1", ProxyType.Any),
                makePermission(from = "Account_1", to = "Account_3", ProxyType.Any),
                makePermission(from = "Account_3", to = "Account_1", ProxyType.Any),
            )
        )

        val result = engine.build()

        assertEquals(
            result,
            listOf(
                makeNode(
                    accountId = "Account_1",
                    nestedNodes = mutableListOf(
                        makeNode(
                            accountId = "Account_2",
                            nestedNodes = makeSingleNode(
                                accountId = "Account_3",
                                path = pathWithAny("Account_1", "Account_2")
                            ),
                            path = pathWithAny("Account_1")
                        ),
                        makeNode(
                            accountId = "Account_3",
                            path = pathWithAny("Account_1")
                        )
                    )
                ),
                makeNode(
                    accountId = "Account_2",
                    nestedNodes = makeSingleNode(
                        accountId = "Account_3",
                        nestedNodes = makeSingleNode(
                            accountId = "Account_1",
                            path = pathWithAny("Account_2", "Account_3")
                        ),
                        path = pathWithAny("Account_2")
                    )
                )
            )
        )
    }

    private fun keysOf(vararg values: String): Set<AccountIdKey> {
        return values.toList()
            .mapToSet { it.intoKey() }
    }

    private fun String.intoKey(): AccountIdKey {
        return AccountIdKey(this.toByteArray())
    }

    private fun makePermission(from: String, to: String, type: ProxyType): ProxyPermission {
        return ProxyPermission(from.intoKey(), to.intoKey(), type)
    }

    private fun makeSingleNode(
        accountId: String,
        permissionType: ProxyType = ProxyType.Any,
        nestedNodes: List<Node> = listOf(),
        path: Map<AccountIdKey, ProxyType> = mapOf()
    ): MutableList<Node> {
        return mutableListOf(makeNode(accountId, permissionType, nestedNodes, path))
    }

    private fun makeNode(
        accountId: String,
        permissionType: ProxyType = ProxyType.Any,
        nestedNodes: List<Node> = listOf(),
        path: Map<AccountIdKey, ProxyType> = mapOf()
    ): Node {
        return Node(accountId.intoKey(), permissionType, nestedNodes, path)
    }

    private fun pathWithAny(
        vararg path: String
    ): Map<AccountIdKey, ProxyType> {
        return path.associateBy { it.intoKey() }
            .mapValues { ProxyType.Any }
    }

    private fun path(
        vararg path: Pair<String, ProxyType>
    ): Map<AccountIdKey, ProxyType> {
        return path.toMap()
            .mapKeys { (accountId, _) -> accountId.intoKey() }
    }
}
