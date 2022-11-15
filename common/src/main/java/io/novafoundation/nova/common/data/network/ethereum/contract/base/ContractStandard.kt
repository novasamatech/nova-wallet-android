package io.novafoundation.nova.common.data.network.ethereum.contract.base

import io.novafoundation.nova.common.data.network.ethereum.contract.base.caller.BatchContractCaller
import io.novafoundation.nova.common.data.network.ethereum.contract.base.caller.ContractCaller
import io.novafoundation.nova.common.data.network.ethereum.contract.base.caller.SingleContractCaller
import io.novafoundation.nova.common.data.network.ethereum.subscribtion.BatchId
import io.novafoundation.nova.core.updater.EthereumSharedRequestsBuilder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName

interface ContractStandard<C> {

    fun query(
        address: String,
        caller: ContractCaller,
        defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameterName.LATEST
    ): C
}

fun <C> ContractStandard<C>.queryBatched(
    address: String,
    batchId: BatchId,
    ethereumSharedRequestsBuilder: EthereumSharedRequestsBuilder,
) = query(address, BatchContractCaller(batchId, ethereumSharedRequestsBuilder))

fun <C> ContractStandard<C>.querySingle(
    address: String,
    web3j: Web3j,
) = query(address, SingleContractCaller(web3j))
