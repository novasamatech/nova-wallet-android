package io.novafoundation.nova.runtime.ethereum.transaction.builder

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigInteger
import org.web3j.abi.datatypes.Function as EvmFunction

class RealEvmTransactionBuilder : EvmTransactionBuilder {

    private var transactionData: EvmTransactionData? = null

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

    class ContractCall(val contractAddress: String, val function: EvmFunction) : EvmTransactionData()
}
