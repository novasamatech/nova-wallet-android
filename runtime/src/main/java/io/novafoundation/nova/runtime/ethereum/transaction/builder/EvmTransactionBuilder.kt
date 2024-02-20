package io.novafoundation.nova.runtime.ethereum.transaction.builder

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.runtime.ethereum.contract.base.ContractStandard
import io.novasama.substrate_sdk_android.runtime.AccountId
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type as EvmType

interface EvmTransactionBuilder {

    fun nativeTransfer(amount: BalanceOf, recipient: AccountId)

    fun contractCall(contractAddress: String, builder: EvmContractCallBuilder.() -> Unit)

    interface EvmContractCallBuilder {

        var function: String

        fun <T> inputParameter(value: EvmType<T>)

        fun <T : EvmType<*>> outputParameter(typeReference: TypeReference<T>)
    }
}

fun EvmTransactionBuilder() = RealEvmTransactionBuilder()

inline fun <reified T : EvmType<*>> EvmTransactionBuilder.EvmContractCallBuilder.outputParameter() {
    val typeReference = object : TypeReference<T>() {}

    outputParameter(typeReference)
}

fun <T> EvmTransactionBuilder.contractCall(
    contractAddress: String,
    contractStandard: ContractStandard<*, T>,
    contractStandardAction: T.() -> Unit
) {
    val contractTransactions = contractStandard.transact(contractAddress, this)

    contractTransactions.contractStandardAction()
}
