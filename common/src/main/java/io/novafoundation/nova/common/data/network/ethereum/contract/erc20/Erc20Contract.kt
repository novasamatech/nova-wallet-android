package io.novafoundation.nova.common.data.network.ethereum.contract.erc20

import io.novafoundation.nova.common.data.network.ethereum.sendSuspend
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.tx.Contract
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

internal class Erc20Contract(
    contractAddress: String,
    web3j: Web3j,
    transactionManager: TransactionManager,
    gasProvider: ContractGasProvider,
) : Contract("", contractAddress, web3j, transactionManager, gasProvider), ReadOnlyErc20 {

    override suspend fun balanceOf(account: String): BigInteger {
        val function = Function(
            /* name = */
            "balanceOf",
            /* inputParameters = */
            listOf(
                Address(account)
            ),
            /* outputParameters = */
            listOf(
                object : TypeReference<Uint256>() {}
            ),
        )

        return executeRemoteCallSingleValueReturn(function, BigInteger::class.java)
            .sendSuspend()
    }
}
