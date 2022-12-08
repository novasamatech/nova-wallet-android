package io.novafoundation.nova.runtime.ethereum.contract.base.caller

import kotlinx.coroutines.future.asDeferred
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import java.util.concurrent.CompletableFuture

interface ContractCaller {

    fun ethCall(
        transaction: Transaction,
        defaultBlockParameter: DefaultBlockParameter,
    ): CompletableFuture<EthCall>
}

suspend fun ContractCaller.ethCallSuspend(
    transaction: Transaction,
    defaultBlockParameter: DefaultBlockParameter,
): EthCall = ethCall(transaction, defaultBlockParameter).asDeferred().await()
