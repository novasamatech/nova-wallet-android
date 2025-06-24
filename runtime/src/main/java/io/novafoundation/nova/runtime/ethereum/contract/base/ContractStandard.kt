package io.novafoundation.nova.runtime.ethereum.contract.base

import io.novafoundation.nova.core.updater.EthereumSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.contract.base.caller.BatchContractCaller
import io.novafoundation.nova.runtime.ethereum.contract.base.caller.ContractCaller
import io.novafoundation.nova.runtime.ethereum.contract.base.caller.SingleContractCaller
import io.novafoundation.nova.runtime.ethereum.subscribtion.BatchId
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName

interface ContractStandard<Q, T> {

    fun query(
        address: String,
        caller: ContractCaller,
        defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameterName.LATEST
    ): Q

    fun transact(
        contractAddress: String,
        transactionBuilder: EvmTransactionBuilder
    ): T
}

fun <C> ContractStandard<C, *>.queryBatched(
    address: String,
    batchId: BatchId,
    ethereumSharedRequestsBuilder: EthereumSharedRequestsBuilder,
): C = query(address, BatchContractCaller(batchId, ethereumSharedRequestsBuilder))

fun <C> ContractStandard<C, *>.querySingle(
    address: String,
    web3j: Web3j,
): C = query(address, SingleContractCaller(web3j))

