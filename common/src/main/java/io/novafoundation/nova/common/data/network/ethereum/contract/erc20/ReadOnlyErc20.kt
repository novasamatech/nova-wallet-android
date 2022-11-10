package io.novafoundation.nova.common.data.network.ethereum.contract.erc20

import org.web3j.abi.TypeDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.websocket.events.Log
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

interface ReadOnlyErc20 {

    class Transfer(val from: Address, val to: Address, val amount: Uint256)

    companion object {
        val TRANSFER_EVENT = Event(
            "Transfer",
            listOf(
                object : TypeReference<Address>(true) {},
                object : TypeReference<Address>(true) {},
                object : TypeReference<Uint256>(false) {}
            )
        )

        fun parseTransferEvent(log: Log): Transfer {
            return Transfer(
                from = TypeDecoder.decodeAddress(log.topics[1]),
                to = TypeDecoder.decodeAddress(log.topics[2]),
                amount = TypeDecoder.decodeNumeric(log.data, Uint256::class.java)
            )
        }
    }

    suspend fun balanceOf(account: String): BigInteger
}

fun ReadOnlyErc20(
    contractAddress: String,
    web3j: Web3j,
): ReadOnlyErc20 {
    return Erc20Contract(
        contractAddress= contractAddress,
        web3j = web3j,
        transactionManager = ReadonlyTransactionManager(web3j, null),
        gasProvider = DefaultGasProvider()
    )
}
