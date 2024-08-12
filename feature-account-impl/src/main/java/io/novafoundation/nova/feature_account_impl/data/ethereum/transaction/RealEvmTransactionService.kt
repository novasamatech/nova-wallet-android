package io.novafoundation.nova.feature_account_impl.data.ethereum.transaction

import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.toEcdsaSignatureData
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionBuilding
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.EvmFee
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireMetaAccountFor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.runtime.ethereum.EvmRpcException
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getCallEthereumApiOrThrow
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
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
    private val gasPriceProviderFactory: GasPriceProviderFactory,
) : EvmTransactionService {

    override suspend fun calculateFee(
        chainId: ChainId,
        origin: TransactionOrigin,
        fallbackGasLimit: BigInteger,
        building: EvmTransactionBuilding
    ): Fee {
        val web3Api = chainRegistry.getCallEthereumApiOrThrow(chainId)
        val chain = chainRegistry.getChain(chainId)

        val submittingMetaAccount = accountRepository.requireMetaAccountFor(origin, chainId)
        val submittingAddress = submittingMetaAccount.requireAddressIn(chain)

        val txBuilder = EvmTransactionBuilder().apply(building)
        val txForFee = txBuilder.buildForFee(submittingAddress)

        val gasPrice = gasPriceProviderFactory.createKnown(chainId).getGasPrice()
        val gasLimit = web3Api.gasLimitOrDefault(txForFee, fallbackGasLimit)

        return EvmFee(
            gasLimit,
            gasPrice,
            SubmissionOrigin.singleOrigin(submittingMetaAccount.requireAccountIdIn(chain)),
            Fee.PaymentAsset.Native
        )
    }

    override suspend fun transact(
        chainId: ChainId,
        presetFee: Fee?,
        origin: TransactionOrigin,
        fallbackGasLimit: BigInteger,
        building: EvmTransactionBuilding
    ): Result<ExtrinsicSubmission> = runCatching {
        val chain = chainRegistry.getChain(chainId)
        val submittingMetaAccount = accountRepository.requireMetaAccountFor(origin, chainId)
        val submittingAddress = submittingMetaAccount.requireAddressIn(chain)
        val submittingAccountId = submittingMetaAccount.requireAccountIdIn(chain)

        val web3Api = chainRegistry.getCallEthereumApiOrThrow(chainId)
        val txBuilder = EvmTransactionBuilder().apply(building)

        val evmFee = presetFee?.castOrNull<EvmFee>() ?: run {
            val txForFee = txBuilder.buildForFee(submittingAddress)
            val gasPrice = gasPriceProviderFactory.createKnown(chainId).getGasPrice()
            val gasLimit = web3Api.gasLimitOrDefault(txForFee, fallbackGasLimit)

            EvmFee(
                gasLimit,
                gasPrice,
                SubmissionOrigin.singleOrigin(submittingAccountId),
                Fee.PaymentAsset.Native
            )
        }

        val nonce = web3Api.getNonce(submittingAddress)

        val txForSign = txBuilder.buildForSign(nonce = nonce, gasPrice = evmFee.gasPrice, gasLimit = evmFee.gasLimit)
        val toSubmit = signTransaction(txForSign, submittingMetaAccount, chain)

        val txHash = web3Api.sendTransaction(toSubmit)

        ExtrinsicSubmission(hash = txHash, submissionOrigin = SubmissionOrigin.singleOrigin(submittingAccountId))
    }

    private suspend fun signTransaction(txForSign: RawTransaction, metaAccount: MetaAccount, chain: Chain): String {
        val ethereumChainId = chain.addressPrefix.toLong()
        val encodedTx = TransactionEncoder.encode(txForSign, ethereumChainId)

        val signer = signerProvider.rootSignerFor(metaAccount)
        val accountId = metaAccount.requireAccountIdIn(chain)

        val signerPayload = SignerPayloadRaw(encodedTx, accountId)
        val signatureData = signer.signRaw(signerPayload).toEcdsaSignatureData()

        val eip155SignatureData: Sign.SignatureData = TransactionEncoder.createEip155SignatureData(signatureData, ethereumChainId)

        return txForSign.encodeWith(eip155SignatureData).toHexString(withPrefix = true)
    }

    private suspend fun Web3Api.getNonce(address: String): BigInteger {
        return ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
            .sendSuspend()
            .transactionCount
    }

    private suspend fun Web3Api.gasLimitOrDefault(tx: Transaction, default: BigInteger): BigInteger = try {
        ethEstimateGas(tx).sendSuspend().amountUsed
    } catch (rpcException: EvmRpcException) {
        default
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
