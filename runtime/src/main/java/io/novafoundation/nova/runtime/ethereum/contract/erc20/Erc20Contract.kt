package io.novafoundation.nova.runtime.ethereum.contract.erc20

import io.novafoundation.nova.common.utils.ethereumAccountIdToAddress
import io.novafoundation.nova.runtime.ethereum.contract.base.CallableContract
import io.novafoundation.nova.runtime.ethereum.contract.base.ContractStandard
import io.novafoundation.nova.runtime.ethereum.contract.base.caller.ContractCaller
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Deferred
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
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
) : Erc20Transactions {

    override fun transfer(recipient: AccountId, amount: BigInteger) {
        evmTransactionsBuilder.contractCall(contractAddress) {
            function = "transfer"

            inputParameter(Address(recipient.ethereumAccountIdToAddress(withChecksum = true)))
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

        return executeCallSingleValueReturnAsync(function, Uint256::getValue)
    }

    override suspend fun symbol(): String {
        val outputParams = listOf(
            object : TypeReference<Utf8String>() {}
        )
        val function = Function("symbol", emptyList(), outputParams)

        return executeCallSingleValueReturnSuspend(function, Utf8String::getValue)
    }

    override suspend fun decimals(): BigInteger {
        val outputParams = listOf(
            object : TypeReference<Uint8>() {}
        )
        val function = Function("decimals", emptyList(), outputParams)

        return executeCallSingleValueReturnSuspend(function, Uint8::getValue)
    }

    override suspend fun totalSupply(): BigInteger {
        val outputParams = listOf(
            object : TypeReference<Uint256>() {}
        )
        val function = Function("totalSupply", emptyList(), outputParams)

        return executeCallSingleValueReturnSuspend(function, Uint256::getValue)
    }
}
