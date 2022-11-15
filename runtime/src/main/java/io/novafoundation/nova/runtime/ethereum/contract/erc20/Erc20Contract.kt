package io.novafoundation.nova.runtime.ethereum.contract.erc20

import io.novafoundation.nova.runtime.ethereum.contract.base.CallableContract
import io.novafoundation.nova.runtime.ethereum.contract.base.ContractStandard
import io.novafoundation.nova.runtime.ethereum.contract.base.caller.ContractCaller
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import kotlinx.coroutines.Deferred
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.DefaultBlockParameter
import java.math.BigInteger

class Erc20Standard : ContractStandard<Erc20Queries, Erc20Transactions> {

    override fun query(address: String, caller: ContractCaller, defaultBlockParameter: DefaultBlockParameter): Erc20Queries {
        return Erc20QueriesImpl(address, caller, defaultBlockParameter)
    }

    override fun transact(contractAddress: String, transactionBuilder: EvmTransactionBuilder): Erc20Transactions {
        return Erc20TransactionsImpl(contractAddress, transactionBuilder)
    }
}

private class Erc20TransactionsImpl(
    private val contractAddress: String,
    private val evmTransactionsBuilder: EvmTransactionBuilder,
): Erc20Transactions {

    override fun transfer(recipient: String, amount: BigInteger) {
        evmTransactionsBuilder.contractCall(contractAddress) {
            function = "transfer"

            inputParameter(Address(recipient))
            inputParameter(Uint256(amount))
        }
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
