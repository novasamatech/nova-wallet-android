package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ethereum.contract.base.querySingle
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Queries
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.multiNetwork.getEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

class Erc20Transfer(
    val txHash: String,
    val blockNumber: String,
    val from: String,
    val to: String,
    val contract: String,
    val amount: BigInteger,
)

class Web3jServiceIntegrationTest : BaseIntegrationTest() {

    @Test
    fun shouldFetchBalance(): Unit = runBlocking {
        val web3j = moonbeamWeb3j()
        val balance = web3j.ethGetBalance("0xf977814e90da44bfa03b6295a0616a897441acec", DefaultBlockParameterName.LATEST).sendSuspend()
        Log.d(LOG_TAG, balance.balance.toString())
    }

    @Test
    fun shouldFetchComplexStructure(): Unit = runBlocking {
        val web3j = moonbeamWeb3j()
        val block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, true).sendSuspend()
        Log.d(LOG_TAG, block.block.hash)
    }

    @Test
    fun shouldSubscribeToNewHeadEvents(): Unit = runBlocking {
        val web3j = moonbeamWeb3j()
        val newHead = web3j.newHeadsNotifications().asFlow().first()

        Log.d(LOG_TAG, "New head appended to chain: ${newHead.params.result.hash}")
    }

    @Test
    fun shouldSubscribeBalances(): Unit = runBlocking {
        val web3j = moonbeamWeb3j()
        val accountAddress = "0x4A43C16107591AE5Ec904e584ed4Bb05386F98f7"
        val moonbeamUsdc = "0x818ec0a7fe18ff94269904fced6ae3dae6d6dc0b"

        val balanceUpdates = web3j.erc20BalanceFlow(accountAddress, moonbeamUsdc).take(2).toList()

        error("Initial balance: ${balanceUpdates.first()}, new balance: ${balanceUpdates.second()}")
    }

    private fun Web3Api.erc20BalanceFlow(account: String, contract: String): Flow<Balance> {
        return flow {
            val erc20 = Erc20Standard().querySingle(contract, web3j = this@erc20BalanceFlow)
            val initialBalance = erc20.balanceOfAsync(account).await()

            emit(initialBalance)

            val changes = accountErcTransfersFlow(account).map {
                erc20.balanceOfAsync(account).await()
            }

            emitAll(changes)
        }
    }


    private fun Web3Api.accountErcTransfersFlow(address: String): Flow<Erc20Transfer> {
        val addressTopic = TypeEncoder.encode(Address(address))

        val transferEvent = Erc20Queries.TRANSFER_EVENT
        val transferEventSignature = EventEncoder.encode(transferEvent)
        val contractAddresses = emptyList<String>() // everything

        val erc20SendTopic = listOf(
            Topic.Single(transferEventSignature), // zero-th topic is event signature
            Topic.AnyOf(addressTopic), // our account as `from`
        )

        val erc20ReceiveTopic = listOf(
            Topic.Single(transferEventSignature), // zero-th topic is event signature
            Topic.Any, // anyone is `from`
            Topic.AnyOf(addressTopic) // out account as `to`
        )

        val receiveTransferNotifications = logsNotifications(contractAddresses, erc20ReceiveTopic)
        val sendTransferNotifications = logsNotifications(contractAddresses, erc20SendTopic)

        val transferNotifications = merge(receiveTransferNotifications, sendTransferNotifications)

        return transferNotifications.map { logNotification ->
            val log = logNotification.params.result

            val contract = log.address
            val event = Erc20Queries.parseTransferEvent(log)

            Erc20Transfer(
                txHash = log.transactionHash,
                blockNumber = log.blockNumber,
                from = event.from.value,
                to = event.to.value,
                contract = contract,
                amount = event.amount.value,
            )
        }
    }

    private suspend fun moonbeamWeb3j(): Web3Api {
        val moonbeamChainId = "fe58ea77779b7abda7da4ec526d14db9b1e9cd40a217c34892af80a9b332b76d"

        return chainRegistry.getEthereumApiOrThrow(moonbeamChainId, Chain.Node.ConnectionType.WSS)
    }
}
