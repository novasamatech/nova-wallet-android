package io.novafoundation.nova.common.data.network.ethereum.contract.base.caller

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall

class SingleContractCaller(
    private val web3j: Web3j
) : ContractCaller {

    override fun ethCall(transaction: Transaction, defaultBlockParameter: DefaultBlockParameter): Deferred<EthCall> {
        val request = web3j.ethCall(transaction, defaultBlockParameter)

        return request.sendAsync().asDeferred()
    }
}
