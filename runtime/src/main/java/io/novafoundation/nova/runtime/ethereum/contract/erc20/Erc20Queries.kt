package io.novafoundation.nova.runtime.ethereum.contract.erc20

import kotlinx.coroutines.Deferred
import org.web3j.abi.TypeDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.websocket.events.Log
import java.math.BigInteger

interface Erc20Queries {

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

    suspend fun balanceOfAsync(account: String): Deferred<BigInteger>

    suspend fun symbol(): String

    suspend fun decimals(): BigInteger

    suspend fun totalSupply(): BigInteger
}
