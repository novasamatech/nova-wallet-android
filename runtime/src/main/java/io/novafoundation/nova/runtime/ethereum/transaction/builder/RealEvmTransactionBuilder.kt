package io.novafoundation.nova.runtime.ethereum.transaction.builder

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.utils.ethereumAccountIdToAddress
import io.novasama.substrate_sdk_android.runtime.AccountId
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigInteger
import org.web3j.abi.datatypes.Function as EvmFunction

class RealEvmTransactionBuilder : EvmTransactionBuilder {

    private var transactionData: EvmTransactionData? = null

    override fun nativeTransfer(amount: BalanceOf, recipient: AccountId) {
        transactionData = EvmTransactionData.NativeTransfer(amount, recipient.ethereumAccountIdToAddress())
    }

    override fun contractCall(contractAddress: String, builder: EvmTransactionBuilder.EvmContractCallBuilder.() -> Unit) {
        transactionData = EvmContractCallBuilder(contractAddress)
            .apply(builder)
            .build()
    }

    fun buildForFee(originAddress: String): Transaction {
        return when (val txData = requireNotNull(transactionData)) {
            is EvmTransactionData.ContractCall -> {
                val data = FunctionEncoder.encode(txData.function)

                Transaction.createFunctionCallTransaction(
                    originAddress,
                    null,
                    null,
                    null,
                    txData.contractAddress,
                    null,
                    data
                )
            }

            is EvmTransactionData.NativeTransfer -> {
                Transaction.createEtherTransaction(
                    originAddress,
                    null,
                    null,
                    null,
                    txData.recipientAddress,
                    txData.amount
                )
            }
        }
    }

    fun buildForSign(
        nonce: BigInteger,
        gasPrice: BigInteger,
        gasLimit: BigInteger
    ): RawTransaction {
        return when (val txData = requireNotNull(transactionData)) {
            is EvmTransactionData.ContractCall -> {
                val data = FunctionEncoder.encode(txData.function)

                RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    txData.contractAddress,
                    null,
                    data
                )
            }

            is EvmTransactionData.NativeTransfer -> {
                RawTransaction.createEtherTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    txData.recipientAddress,
                    txData.amount
                )
            }
        }
    }
}

private class EvmContractCallBuilder(
    private val contractAddress: String
) : EvmTransactionBuilder.EvmContractCallBuilder {

    private var _function: String? = null
    private var input: MutableList<Type<*>> = mutableListOf()
    private var outputParameters: MutableList<TypeReference<*>> = mutableListOf()

    override var function: String
        get() = requireNotNull(_function)
        set(value) {
            _function = value
        }

    override fun <T> inputParameter(value: Type<T>) {
        input += value
    }

    override fun <T : Type<*>> outputParameter(typeReference: TypeReference<T>) {
        outputParameters += typeReference
    }

    fun build(): EvmTransactionData.ContractCall {
        return EvmTransactionData.ContractCall(
            contractAddress = contractAddress,
            function = EvmFunction(function, input, outputParameters)
        )
    }
}

private sealed class EvmTransactionData {

    class NativeTransfer(val amount: BalanceOf, val recipientAddress: String) : EvmTransactionData()

    class ContractCall(val contractAddress: String, val function: EvmFunction) : EvmTransactionData()
}
