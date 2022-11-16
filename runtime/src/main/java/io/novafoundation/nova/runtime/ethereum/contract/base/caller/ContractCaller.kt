package io.novafoundation.nova.runtime.ethereum.contract.base.caller

import kotlinx.coroutines.Deferred
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall

interface ContractCaller {

    fun ethCall(
        transaction: Transaction,
        defaultBlockParameter: DefaultBlockParameter,
    ): Deferred<EthCall>
}
