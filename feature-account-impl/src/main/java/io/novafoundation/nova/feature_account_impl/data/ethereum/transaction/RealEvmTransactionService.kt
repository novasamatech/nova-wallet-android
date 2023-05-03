package io.novafoundation.nova.feature_account_impl.data.ethereum.transaction

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionBuilding
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionHash
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.runtime.ethereum.EvmRpcException
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.awaitCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import java.math.BigInteger

internal class RealEvmTransactionService(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val signerProvider: SignerProvider,
) : EvmTransactionService {

    override suspend fun calculateFee(
        chainId: ChainId,
        origin: TransactionOrigin,
        fallbackGasLimit: BigInteger,
        building: EvmTransactionBuilding
    ): BigInteger {
        val web3Api = chainRegistry.awaitCallEthereumApiOrThrow(chainId)
        val chain = chainRegistry.getChain(chainId)

        val submittingMetaAccount = findMetaAccountFor(origin)
        val submittingAddress = submittingMetaAccount.requireAddressIn(chain)

        val txBuilder = EvmTransactionBuilder().apply(building)
        val txForFee = txBuilder.buildForFee(submittingAddress)

        val gasPrice = web3Api.gasPrice()

        return gasPrice * web3Api.gasLimitOrDefault(txForFee, fallbackGasLimit)
    }

    override suspend fun transact(
        chainId: ChainId,
        origin: TransactionOrigin,
        fallbackGasLimit: BigInteger,
        building: EvmTransactionBuilding
    ): Result<TransactionHash> = runCatching {
        val chain = chainRegistry.getChain(chainId)
        val submittingMetaAccount = findMetaAccountFor(origin)
        val submittingAddress = submittingMetaAccount.requireAddressIn(chain)

        val web3Api = chainRegistry.awaitCallEthereumApiOrThrow(chainId)
        val txBuilder = EvmTransactionBuilder().apply(building)
        val txForFee = txBuilder.buildForFee(submittingAddress)

        val gasPrice = web3Api.gasPrice()
        val gasLimit = web3Api.gasLimitOrDefault(txForFee, fallbackGasLimit)
        val nonce = web3Api.getNonce(submittingAddress)

        val txForSign = txBuilder.buildForSign(nonce = nonce, gasPrice = gasPrice, gasLimit = gasLimit)
        val toSubmit = signTransaction(txForSign, submittingMetaAccount, chain)

        web3Api.sendTransaction(toSubmit)
    }

    private suspend fun signTransaction(txForSign: RawTransaction, metaAccount: MetaAccount, chain: Chain): String {
        val ethereumChainId = chain.addressPrefix.toLong()
        val encodedTx = TransactionEncoder.encode(txForSign, ethereumChainId)

        val signer = signerProvider.signerFor(metaAccount)
        val accountId = metaAccount.requireAccountIdIn(chain)

        val signerPayload = SignerPayloadRaw(encodedTx, accountId)
        val signatureData = signer.signRaw(signerPayload).toSignatureData()

        val eip155SignatureData: Sign.SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, ethereumChainId)

        return txForSign.encodeWith(eip155SignatureData).toHexString(withPrefix = true)
    }

    private suspend fun findMetaAccountFor(origin: TransactionOrigin): MetaAccount {
        return when (origin) {
            TransactionOrigin.SelectedWallet -> accountRepository.getSelectedMetaAccount()
        }
    }

    private suspend fun Web3Api.getNonce(address: String): BigInteger {
        return ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
            .sendSuspend()
            .transactionCount
    }

    private suspend fun Web3Api.gasPrice(): BigInteger = ethGasPrice().sendSuspend().gasPrice

    private suspend fun Web3Api.gasLimitOrDefault(tx: Transaction, default: BigInteger): BigInteger = try {
        ethEstimateGas(tx).sendSuspend().amountUsed
    } catch (rpcException: EvmRpcException) {
        default
    }

    private fun SignatureWrapper.toSignatureData(): Sign.SignatureData {
        require(this is SignatureWrapper.Ecdsa)

        return Sign.SignatureData(v, r, s)
    }

    private fun RawTransaction.encodeWith(signatureData: Sign.SignatureData): ByteArray {
        val values = TransactionEncoder.asRlpValues(this, signatureData)
        val rlpList = RlpList(values)
        return RlpEncoder.encode(rlpList)
    }

    private suspend fun Web3Api.sendTransaction(transactionData: String): String {
        return ethSendRawTransaction(transactionData).sendSuspend().transactionHash
    }
}
