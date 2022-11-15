package io.novafoundation.nova.runtime.ethereum.contract.base

import io.novafoundation.nova.runtime.ethereum.contract.base.caller.ContractCaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.tx.TransactionManager
import org.web3j.tx.exceptions.ContractCallException
import kotlin.coroutines.coroutineContext

open class CallableContract(
    protected val contractAddress: String,
    protected val contractCaller: ContractCaller,
    protected val defaultBlockParameter: DefaultBlockParameter,
) {

    @Suppress("UNCHECKED_CAST")
    protected suspend fun <T : Type<*>?, R> executeCallSingleValueReturn(
        function: Function,
        extractResult: (T) -> R
    ): Deferred<R> {
        val encodedFunction = FunctionEncoder.encode(function)
        val tx = Transaction.createEthCallTransaction(null, contractAddress, encodedFunction)

        val ethCallDeferred = contractCaller.ethCall(tx, defaultBlockParameter)

        return CoroutineScope(coroutineContext).async {
            val ethCall = ethCallDeferred.await()
            assertCallNotReverted(ethCall)

            val args = FunctionReturnDecoder.decode(ethCall.value, function.outputParameters)
            val type = args.first() as T

            extractResult(type)
        }
    }

    private fun assertCallNotReverted(ethCall: EthCall) {
        if (ethCall.isReverted) {
            throw ContractCallException(String.format(TransactionManager.REVERT_ERR_STR, ethCall.revertReason))
        }
    }
}
