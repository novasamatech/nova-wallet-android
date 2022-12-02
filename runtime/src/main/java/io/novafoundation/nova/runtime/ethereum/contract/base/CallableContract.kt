package io.novafoundation.nova.runtime.ethereum.contract.base

import io.novafoundation.nova.runtime.ethereum.contract.base.caller.ContractCaller
import io.novafoundation.nova.runtime.ethereum.contract.base.caller.ethCallSuspend
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.tx.TransactionManager
import org.web3j.tx.exceptions.ContractCallException

open class CallableContract(
    protected val contractAddress: String,
    protected val contractCaller: ContractCaller,
    protected val defaultBlockParameter: DefaultBlockParameter,
) {

    @Suppress("UNCHECKED_CAST")
    protected fun <T : Type<*>?, R> executeCallSingleValueReturnAsync(
        function: Function,
        extractResult: (T) -> R,
    ): Deferred<R> {
        val tx = createTx(function)

        return contractCaller.ethCall(tx, defaultBlockParameter).thenApply { ethCall ->
            processEthCallResponse(ethCall, function, extractResult)
        }.asDeferred()
    }

    @Suppress("UNCHECKED_CAST")
    protected suspend fun <T : Type<*>?, R> executeCallSingleValueReturnSuspend(
        function: Function,
        extractResult: (T) -> R,
    ): R {
        val tx = createTx(function)
        val ethCall = contractCaller.ethCallSuspend(tx, defaultBlockParameter)

        return processEthCallResponse(ethCall, function, extractResult)
    }

    private fun <R, T : Type<*>?> processEthCallResponse(
        ethCall: EthCall,
        function: Function,
        extractResult: (T) -> R
    ): R {
        assertCallNotReverted(ethCall)

        val args = FunctionReturnDecoder.decode(ethCall.value, function.outputParameters)
        val type = args.first() as T

        return extractResult(type)
    }

    private fun createTx(function: Function): Transaction {
        val encodedFunction = FunctionEncoder.encode(function)
        return Transaction.createEthCallTransaction(null, contractAddress, encodedFunction)
    }

    private fun assertCallNotReverted(ethCall: EthCall) {
        if (ethCall.isReverted) {
            throw ContractCallException(String.format(TransactionManager.REVERT_ERR_STR, ethCall.revertReason))
        }
    }
}
