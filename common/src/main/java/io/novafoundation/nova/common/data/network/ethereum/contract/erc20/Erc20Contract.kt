package io.novafoundation.nova.common.data.network.ethereum.contract.erc20

import io.novafoundation.nova.common.data.network.ethereum.contract.base.CallableContract
import io.novafoundation.nova.common.data.network.ethereum.contract.base.ContractStandard
import io.novafoundation.nova.common.data.network.ethereum.contract.base.caller.ContractCaller
import kotlinx.coroutines.Deferred
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.DefaultBlockParameter
import java.math.BigInteger

class Erc20Standard : ContractStandard<Erc20Queries> {

    override fun query(address: String, caller: ContractCaller, defaultBlockParameter: DefaultBlockParameter): Erc20Queries {
        return Erc20QueriesImpl(address, caller, defaultBlockParameter)
    }
}

private class Erc20QueriesImpl(
    contractAddress: String,
    caller: ContractCaller,
    blockParameter: DefaultBlockParameter,
) : CallableContract(contractAddress, caller, blockParameter), Erc20Queries {

    override suspend fun balanceOfAsync(account: String): Deferred<BigInteger> {
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

        return executeCallSingleValueReturn(function, Uint256::getValue)
    }
}
