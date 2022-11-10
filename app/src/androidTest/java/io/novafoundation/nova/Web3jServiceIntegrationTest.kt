package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.data.network.ethereum.WebSocketWeb3jService
import io.novafoundation.nova.common.data.network.ethereum.sendSuspend
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.runtime.multiNetwork.awaitSocket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName


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


    private suspend fun moonbeamWeb3j(): Web3j {
        val moonbeamChainId = "fe58ea77779b7abda7da4ec526d14db9b1e9cd40a217c34892af80a9b332b76d"
        val moonbeamSocket = chainRegistry.awaitSocket(moonbeamChainId)
        val moonbeamService = WebSocketWeb3jService(moonbeamSocket)

        return Web3j.build(moonbeamService)
    }
}
