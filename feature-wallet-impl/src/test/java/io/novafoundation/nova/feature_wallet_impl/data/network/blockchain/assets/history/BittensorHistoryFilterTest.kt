package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import com.google.gson.Gson
import io.novafoundation.nova.feature_wallet_api.domain.model.BittensorServiceFeeRecipients
import io.novafoundation.nova.feature_wallet_impl.data.network.model.response.SubqueryHistoryElementResponse.Query.HistoryElements.Node
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

/** Synthetic SubQuery fixtures only; live indexer row co-occurrence is unproven. */
class BittensorHistoryFilterTest {

    private val chain = mock(Chain::class.java)

    private val asset = mock(Chain.Asset::class.java)

    private val gson = Gson()

    private val userAddress = ByteArray(32) { 1 }.toAddress(42)

    private val subnetAddress = "modlsubtensr".toByteArray().copyOf(32).toAddress(42)

    private val hash = "0x" + "ab".repeat(32)

    private val beneficiary = BittensorServiceFeeRecipients.CURRENT_ADDRESS

    @Before
    fun setup() {
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(asset.type).thenReturn(Chain.Asset.Type.Native)
        `when`(asset.staking).thenReturn(listOf(Chain.Asset.StakingType.BITTENSOR_ROOT))
    }

    @Test
    fun `successful subnet transfers are hidden only with matching direct stake evidence`() {
        for (call in listOf("addStake", "add_stake_limit", "removeStake", "remove_stake_limit", "removeStakeFullLimit")) {
            val parent = parent(call = call)
            val incoming = transfer(from = subnetAddress, to = userAddress)
            val outgoing = transfer(from = userAddress, to = subnetAddress)

            assertEquals(listOf(parent), filter(listOf(parent, incoming, outgoing)))
        }
    }

    @Test
    fun `standalone transfer to subnet account stays visible with or without hash`() {
        val transfer = transfer()
        val standaloneParent = parent(module = "balances", call = "transferKeepAlive")
        assertEquals(listOf(transfer), filter(listOf(transfer)))
        assertEquals(listOf(standaloneParent, transfer), filter(listOf(standaloneParent, transfer)))
        val unhashed = transfer(hash = null)
        assertEquals(listOf(unhashed), filter(listOf(unhashed)))
    }

    @Test
    fun `failed transfer and failed parent remain visible`() {
        val failedTransfer = transfer(success = false)
        val parent = parent()
        assertEquals(listOf(parent, failedTransfer), filter(listOf(parent, failedTransfer)))
        val failedParent = parent(success = false)
        val transfer = transfer()
        assertEquals(listOf(failedParent, transfer), filter(listOf(failedParent, transfer)))
    }

    @Test
    fun `other block account hash or missing parent cannot establish technical origin`() {
        val transfer = transfer()
        for (parent in listOf(parent(block = 11), parent(account = "other"), parent(hash = "0x" + "cd".repeat(32)))) {
            assertEquals(listOf(parent, transfer), filter(listOf(parent, transfer)))
        }
        for (invalidHash in listOf(null, "", "0x", "not-hex", "0x12")) {
            val parent = parent(hash = invalidHash)
            val candidate = transfer(hash = invalidHash)
            assertEquals(listOf(parent, candidate), filter(listOf(parent, candidate)))
        }
    }

    @Test
    fun `utility proxy multisig and unknown calls are not inferred to be staking`() {
        val transfer = transfer()
        for (parent in listOf(
            parent(module = "utility", call = "batchAll"),
            parent(module = "proxy", call = "proxy"),
            parent(module = "multisig", call = "asMulti"),
            parent(call = "unknownCall")
        )) {
            assertEquals(listOf(parent, transfer), filter(listOf(parent, transfer)))
        }
    }

    @Test
    fun `successful standalone fee recipient transfers are hidden without transaction provenance`() {
        for (hash in listOf(this.hash, null, "", "malformed")) {
            val transfer = transfer(to = beneficiary, hash = hash)
            assertEquals(emptyList<Node>(), filter(listOf(transfer)))
        }
        val ordinaryParent = parent(module = "balances", call = "transferKeepAlive")
        assertEquals(listOf(ordinaryParent), filter(listOf(ordinaryParent, transfer(to = beneficiary))))
    }

    @Test
    fun `fee recipient rule does not depend on matching or successful parent`() {
        val transfer = transfer(to = beneficiary)
        for (parent in listOf(parent(), parent(module = "utility", call = "batchAll"), parent(success = false), parent(block = 11))) {
            assertEquals(listOf(parent), filter(listOf(parent, transfer)))
        }
    }

    @Test
    fun `failed fee recipient transfer and transfer from recipient stay visible`() {
        val failed = transfer(to = beneficiary, success = false)
        val outgoing = transfer(from = beneficiary, to = userAddress)
        val ordinary = transfer(to = userAddress)
        val nodes = listOf(failed, outgoing, ordinary)
        assertEquals(nodes, filter(nodes))
    }

    @Test
    fun `current and configured historical recipients are matched by account ID`() {
        // iOS currently has no historical addresses; this fixture covers a future rotation.
        val historical = ByteArray(32) { 7 }.toAddress(42)
        val addresses = listOf(beneficiary, historical)
        val currentTransfer = transfer(to = beneficiary.toAccountId().toAddress(0))
        val historicalTransfer = transfer(to = historical.toAccountId().toAddress(2))
        val ordinary = transfer(to = userAddress)
        val nodes = listOf(currentTransfer, historicalTransfer, ordinary)

        assertEquals(listOf(ordinary), filterBittensorTechnicalTransfers(nodes, chain, asset, addresses))
        assertEquals(listOf(historicalTransfer, ordinary), filter(nodes))
    }

    @Test
    fun `invalid configured recipients cannot hide unrelated transfers`() {
        val nodes = listOf(transfer(to = userAddress), transfer(to = "invalid"))
        assertEquals(nodes, filterBittensorTechnicalTransfers(nodes, chain, asset, listOf("", "invalid")))
    }

    @Test
    fun `fee recipient rule preserves other chains assets and non Bittensor staking`() {
        val nodes = listOf(transfer(to = beneficiary))
        `when`(chain.id).thenReturn("other-chain")
        assertEquals(nodes, filter(nodes))
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(asset.type).thenReturn(Chain.Asset.Type.Unsupported)
        assertEquals(nodes, filter(nodes))
        `when`(asset.type).thenReturn(Chain.Asset.Type.Native)
        `when`(asset.staking).thenReturn(emptyList())
        assertEquals(nodes, filter(nodes))
    }

    @Test
    fun `fee recipient asset transfer rows remain visible`() {
        val assetTransfer = Node.AssetTransfer("1", BigInteger.TEN, beneficiary, userAddress, BigInteger.ZERO, true)
        val node = Node("asset-transfer", 1, hash, userAddress, null, 10, null, null, null, assetTransfer, null)
        assertEquals(listOf(node), filter(listOf(node)))
    }

    @Test
    fun `malformed address and other chains or nonnative assets are retained`() {
        val parent = parent()
        val malformed = transfer(to = "invalid")
        assertEquals(listOf(parent, malformed), filter(listOf(parent, malformed)))
        val transfer = transfer()
        val nodes = listOf(parent, transfer)
        `when`(chain.id).thenReturn("other-chain")
        assertEquals(nodes, filter(nodes))
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(asset.type).thenReturn(Chain.Asset.Type.Unsupported)
        assertEquals(nodes, filter(nodes))
    }

    @Test
    fun `conflicting parent evidence preserves the transfer`() {
        val nodes = listOf(parent(), parent(module = "balances", call = "transfer"), transfer())
        assertEquals(nodes, filter(nodes))
    }

    @Test
    fun `existing response fields decode required evidence without invented nested calls`() {
        val json = """[
            {"id":"parent","timestamp":1,"blockNumber":10,"address":"$userAddress","extrinsicHash":"$hash",
             "extrinsic":{"module":"subtensorModule","call":"addStake","fee":"1","success":true}},
            {"id":"transfer","timestamp":1,"blockNumber":10,"address":"$userAddress","extrinsicHash":"$hash",
             "transfer":{"from":"$userAddress","to":"$subnetAddress","amount":"10","fee":"0","success":true}}
        ]"""
        val nodes = gson.fromJson(json, Array<Node>::class.java).toList()

        assertEquals(listOf("parent"), filter(nodes).map { it.id })
        // A transfer-only page has no evidence about its enclosing call.
        assertEquals(listOf(nodes[1]), filter(listOf(nodes[1])))
        val withoutBlocks = gson.fromJson(json.replace("\"blockNumber\":10,", ""), Array<Node>::class.java).toList()
        assertEquals(withoutBlocks, filter(withoutBlocks))
    }

    private fun filter(nodes: List<Node>) = filterBittensorTechnicalTransfers(nodes, chain, asset)

    private fun parent(
        module: String = "subtensorModule",
        call: String = "addStake",
        success: Boolean = true,
        hash: String? = this.hash,
        block: Long = 10,
        account: String = userAddress
    ): Node {
        val extrinsic = Node.Extrinsic(module, call, BigInteger.ONE, success)
        return node("parent", hash, block, account, extrinsic = extrinsic)
    }

    private fun transfer(
        from: String = userAddress,
        to: String = subnetAddress,
        success: Boolean = true,
        hash: String? = this.hash
    ): Node {
        val transfer = Node.Transfer(BigInteger.TEN, to, from, BigInteger.ZERO, success)
        return node("transfer", hash, 10, userAddress, transfer = transfer)
    }

    private fun node(
        id: String,
        hash: String?,
        block: Long,
        account: String,
        transfer: Node.Transfer? = null,
        extrinsic: Node.Extrinsic? = null
    ) = Node(id, 1, hash, account, null, block, null, transfer, extrinsic, null, null)
}
