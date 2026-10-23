package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import com.google.gson.Gson
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.domain.model.BittensorServiceFeeRecipients
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
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
    fun `successful subnet transfers inside an extrinsic are hidden whatever the enclosing call`() {
        for (parent in listOf(
            parent(call = "addStake"),
            parent(call = "remove_stake_limit"),
            parent(module = "utility", call = "batchAll"),
            parent(module = "proxy", call = "proxy"),
            parent(success = false)
        )) {
            val incoming = transfer(from = subnetAddress, to = userAddress)
            val outgoing = transfer(from = userAddress, to = subnetAddress)

            assertEquals(listOf(parent), filter(listOf(parent, incoming, outgoing)))
        }
    }

    @Test
    fun `hashed subnet transfer is hidden even without its extrinsic on the page`() {
        assertEquals(emptyList<Node>(), filter(listOf(transfer())))
    }

    @Test
    fun `subnet transfer without a valid extrinsic hash stays visible`() {
        for (invalidHash in listOf(null, "", "0x", "not-hex", "0x12", "0x" + "zz".repeat(32))) {
            val candidate = transfer(hash = invalidHash)
            assertEquals(listOf(candidate), filter(listOf(candidate)))
        }
    }

    @Test
    fun `failed subnet transfer and ordinary transfers remain visible`() {
        val failedTransfer = transfer(success = false)
        val ordinary = transfer(to = ByteArray(32) { 3 }.toAddress(42))
        val parent = parent()
        assertEquals(listOf(parent, failedTransfer, ordinary), filter(listOf(parent, failedTransfer, ordinary)))
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
    fun `existing response fields decode the extrinsic hash evidence`() {
        val json = """[
            {"id":"parent","timestamp":1,"blockNumber":10,"address":"$userAddress","extrinsicHash":"$hash",
             "extrinsic":{"module":"subtensorModule","call":"addStake","fee":"1","success":true}},
            {"id":"transfer","timestamp":1,"blockNumber":10,"address":"$userAddress","extrinsicHash":"$hash",
             "transfer":{"from":"$userAddress","to":"$subnetAddress","amount":"10","fee":"0","success":true}}
        ]"""
        val nodes = gson.fromJson(json, Array<Node>::class.java).toList()

        assertEquals(listOf("parent"), filter(nodes).map { it.id })
        assertEquals(emptyList<Node>(), filter(listOf(nodes[1])))
        val withoutHashes = gson.fromJson(json.replace("\"extrinsicHash\":\"$hash\",", ""), Array<Node>::class.java).toList()
        assertEquals(withoutHashes, filter(withoutHashes))
    }

    @Test
    fun `realtime fee recipient and subnet transfers are not stored`() {
        val fee = realtimeTransfer(to = beneficiary.toAccountId())
        val subnetLeg = realtimeTransfer(to = subnetAddress.toAccountId())
        val ordinary = realtimeTransfer(to = ByteArray(32) { 3 })

        assertEquals(listOf(ordinary), filterRealtime(listOf(fee, subnetLeg, ordinary)))
    }

    @Test
    fun `failed realtime fee transfer and other chains keep realtime transfers`() {
        val failedFee = realtimeTransfer(to = beneficiary.toAccountId(), status = Operation.Status.FAILED)
        assertEquals(listOf(failedFee), filterRealtime(listOf(failedFee)))

        val fee = realtimeTransfer(to = beneficiary.toAccountId())
        `when`(chain.id).thenReturn("other-chain")
        assertEquals(listOf(fee), filterRealtime(listOf(fee)))
    }

    private fun filter(nodes: List<Node>) = filterBittensorTechnicalTransfers(nodes, chain, asset)

    private fun filterRealtime(updates: List<RealtimeHistoryUpdate>) = filterBittensorTechnicalRealtimeTransfers(updates, chain, asset)

    private fun realtimeTransfer(
        to: ByteArray,
        status: Operation.Status = Operation.Status.COMPLETED
    ): RealtimeHistoryUpdate {
        val transfer = RealtimeHistoryUpdate.Type.Transfer(userAddress.toAccountId(), to, BigInteger.TEN, asset)
        return RealtimeHistoryUpdate(hash, status, transfer)
    }

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
