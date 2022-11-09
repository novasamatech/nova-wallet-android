package io.novafoundation.nova.feature_dapp_impl.data.network.ethereum

import io.novafoundation.nova.common.data.network.ethereum.sendSuspend
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw
import okhttp3.OkHttpClient
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign.SignatureData
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

interface EthereumApi {

    suspend fun formTransaction(
        fromAddress: String,
        toAddress: String,
        data: String?,
        value: BigInteger?,
    ): RawTransaction

    suspend fun sendTransaction(
        transaction: RawTransaction,
        signer: Signer,
        accountId: AccountId,
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

    override suspend fun formTransaction(fromAddress: String, toAddress: String, data: String?, value: BigInteger?): RawTransaction {
        val nonce = getNonce(fromAddress)
        val gasPrice = getGasPrice()

        val dataOrDefault = data.orEmpty()

        val forFeeEstimatesTx = Transaction.createFunctionCallTransaction(
            fromAddress,
            nonce,
            null,
            null,
            toAddress,
            value,
            dataOrDefault
        )

        val gasLimit = estimateGasLimit(forFeeEstimatesTx)

        return RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            toAddress,
            value,
            dataOrDefault
        )
    }

    /**
     * Ethereum signing is adopted from [TransactionEncoder.signMessage] and [RawTransactionManager.sign]
     */
    override suspend fun sendTransaction(
        transaction: RawTransaction,
        signer: Signer,
        accountId: AccountId,
        ethereumChainId: Long,
    ): String {
        val encodedTx = TransactionEncoder.encode(transaction, ethereumChainId)
        val signerPayload = SignerPayloadRaw(encodedTx, accountId)
        val signatureData = signer.signRaw(signerPayload).toSignatureData()

        val eip155SignatureData: SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, ethereumChainId)
        val transactionData = transaction.encodeWith(eip155SignatureData).toHexString(withPrefix = true)

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

    private fun SignatureWrapper.toSignatureData(): SignatureData {
        require(this is SignatureWrapper.Ecdsa)

        return SignatureData(v, r, s)
    }

    private fun RawTransaction.encodeWith(signatureData: SignatureData): ByteArray {
        val values = TransactionEncoder.asRlpValues(this, signatureData)
        val rlpList = RlpList(values)
        return RlpEncoder.encode(rlpList)
    }
}
