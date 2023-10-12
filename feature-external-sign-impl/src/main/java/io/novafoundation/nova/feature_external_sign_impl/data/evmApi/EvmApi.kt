package io.novafoundation.nova.feature_external_sign_impl.data.evmApi

import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProvider
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChainSource
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChainSource.UnknownChainOptions
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.findEvmCallApi
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

interface EvmApi {

    suspend fun formTransaction(
        fromAddress: String,
        toAddress: String,
        data: String?,
        value: BigInteger?,
        nonce: BigInteger? = null,
        gasLimit: BigInteger? = null,
        gasPrice: BigInteger? = null,
    ): RawTransaction

    /**
     * @return hash of submitted transaction
     */
    suspend fun sendTransaction(
        transaction: RawTransaction,
        signer: Signer,
        accountId: AccountId,
        ethereumChainId: Long,
    ): String

    /**
     * @return signed transaction, ready to be send by eth_sendRawTransaction
     */
    suspend fun signTransaction(
        transaction: RawTransaction,
        signer: Signer,
        accountId: AccountId,
        ethereumChainId: Long,
    ): String

    suspend fun getAccountBalance(address: String): BigInteger

    fun shutdown()
}

class EvmApiFactory(
    private val okHttpClient: OkHttpClient,
    private val chainRegistry: ChainRegistry,
    private val gasPriceProviderFactory: GasPriceProviderFactory,
) {

    suspend fun create(chainSource: EvmChainSource): EvmApi? {
        val knownWeb3jApi = chainRegistry.findEvmCallApi(chainSource.evmChainId)
        val unknownChainOptions = chainSource.unknownChainOptions

        return when {
            knownWeb3jApi != null -> {
                Web3JEvmApi(
                    web3 = knownWeb3jApi,
                    shouldShutdown = false,
                    gasPriceProvider = gasPriceProviderFactory.create(knownWeb3jApi)
                )
            }

            unknownChainOptions is UnknownChainOptions.WithFallBack -> {
                val web3Api = createWeb3j(unknownChainOptions.evmChain.rpcUrl)

                Web3JEvmApi(
                    web3 = web3Api,
                    shouldShutdown = true,
                    gasPriceProvider = gasPriceProviderFactory.create(web3Api)
                )
            }

            else -> null
        }
    }

    private fun createWeb3j(url: String): Web3j {
        return Web3j.build(HttpService(url, okHttpClient))
    }
}

private class Web3JEvmApi(
    private val web3: Web3j,
    private val shouldShutdown: Boolean,
    private val gasPriceProvider: GasPriceProvider,
) : EvmApi {

    override suspend fun formTransaction(
        fromAddress: String,
        toAddress: String,
        data: String?,
        value: BigInteger?,
        nonce: BigInteger?,
        gasLimit: BigInteger?,
        gasPrice: BigInteger?,
    ): RawTransaction {
        val finalNonce = nonce ?: getNonce(fromAddress)
        val finalGasPrice = gasPrice ?: gasPriceProvider.getGasPrice()

        val dataOrDefault = data.orEmpty()

        val finalGasLimit = gasLimit ?: run {
            val forFeeEstimatesTx = Transaction.createFunctionCallTransaction(
                fromAddress,
                finalNonce,
                null,
                null,
                toAddress,
                value,
                dataOrDefault
            )

            estimateGasLimit(forFeeEstimatesTx)
        }

        return RawTransaction.createTransaction(
            finalNonce,
            finalGasPrice,
            finalGasLimit,
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
        val signedRawTransaction = signTransaction(transaction, signer, accountId, ethereumChainId)

        return sendTransaction(signedRawTransaction)
    }

    override suspend fun signTransaction(
        transaction: RawTransaction,
        signer: Signer,
        accountId: AccountId,
        ethereumChainId: Long
    ): String {
        val encodedTx = TransactionEncoder.encode(transaction, ethereumChainId)
        val signerPayload = SignerPayloadRaw(encodedTx, accountId)
        val signatureData = signer.signRaw(signerPayload).toSignatureData()

        val eip155SignatureData: SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, ethereumChainId)

        return transaction.encodeWith(eip155SignatureData).toHexString(withPrefix = true)
    }

    override suspend fun getAccountBalance(address: String): BigInteger {
        return web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendSuspend().balance
    }

    override fun shutdown() {
        if (shouldShutdown) web3.shutdown()
    }

    private suspend fun sendTransaction(transactionData: String): String {
        return web3.ethSendRawTransaction(transactionData).sendSuspend().transactionHash
    }

    private suspend fun getNonce(address: String): BigInteger {
        return web3.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
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
