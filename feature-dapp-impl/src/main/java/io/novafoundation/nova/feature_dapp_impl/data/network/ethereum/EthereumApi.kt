package io.novafoundation.nova.feature_dapp_impl.data.network.ethereum

import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import kotlinx.coroutines.future.asDeferred
import okhttp3.OkHttpClient
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

interface EthereumApi {

    suspend fun formTransaction(
        fromAddress: String,
        toAddress: String,
        data: String,
        value: BigInteger?,
    ): RawTransaction

    suspend fun sendTransaction(
        transaction: RawTransaction,
        keypair: Keypair,
        ethereumChainId: Long,
    ): String

    suspend fun getAccountBalance(address: String): BigInteger

    fun shutdown()
}

class EthereumApiFactory(private val okHttpClient: OkHttpClient) {

    fun create(nodeUrl: String): EthereumApi = Web3JEthereumApi(okHttpClient, nodeUrl)
}

private class Web3JEthereumApi(
    private val okHttpClient: OkHttpClient,
    nodeUrl: String,
) : EthereumApi {

    val web3 = createWeb3j(nodeUrl)

    private fun createWeb3j(url: String): Web3j {
        return Web3j.build(HttpService(url, okHttpClient))
    }

    override suspend fun formTransaction(fromAddress: String, toAddress: String, data: String, value: BigInteger?): RawTransaction {
        val nonce = getNonce(fromAddress)
        val gasPrice = getGasPrice()

        val forFeeEstimatesTx = Transaction.createFunctionCallTransaction(
            fromAddress,
            nonce,
            null,
            null,
            toAddress,
            value,
            data
        )

        val gasLimit = estimateGasLimit(forFeeEstimatesTx)

        return RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            toAddress,
            value,
            data
        )
    }

    override suspend fun sendTransaction(
        transaction: RawTransaction,
        keypair: Keypair,
        ethereumChainId: Long,
    ): String {
        val credentials = Credentials.create(ECKeyPair.create(keypair.privateKey))
        val transactionManager = RawTransactionManager(web3, credentials, ethereumChainId)
        val transactionData = transactionManager.sign(transaction)

        return sendTransaction(transactionData)
    }

    override suspend fun getAccountBalance(address: String): BigInteger {
        return web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendSuspend().balance
    }

    override fun shutdown() {
        web3.shutdown()
    }

    private suspend fun sendTransaction(transactionData: String): String {
        return web3.ethSendRawTransaction(transactionData).sendSuspend().transactionHash
    }

    private suspend fun getGasPrice(): BigInteger {
        return web3.ethGasPrice().sendSuspend().gasPrice
    }

    private suspend fun getNonce(address: String): BigInteger {
        return web3.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
            .sendSuspend()
            .transactionCount
    }

    private suspend fun estimateGasLimit(tx: Transaction): BigInteger {
        return web3.ethEstimateGas(tx).sendSuspend().amountUsed
    }

    private suspend fun <S, T : Response<*>> Request<S, T>.sendSuspend() = sendAsync().asDeferred().await()
}
