package io.novafoundation.nova.runtime.ethereum.contract.erc20

import kotlinx.coroutines.Deferred
import org.web3j.abi.EventEncoder
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

        fun transferEventSignature(): String {
            return EventEncoder.encode(TRANSFER_EVENT)
        }

        fun parseTransferEvent(log: Log): Transfer {
            return parseTransferEvent(
                topic1 = log.topics[1],
                topic2 = log.topics[2],
                data = log.data
            )
        }

        fun parseTransferEvent(
            topic1: String,
            topic2: String,
            data: String
        ): Transfer {
            return Transfer(
                from = TypeDecoder.decodeAddress(topic1),
                to = TypeDecoder.decodeAddress(topic2),
                amount = TypeDecoder.decodeNumeric(data, Uint256::class.java)
            )
        }
    }

    suspend fun balanceOfAsync(account: String): Deferred<BigInteger>

    suspend fun symbol(): String

    suspend fun decimals(): BigInteger

    suspend fun totalSupply(): BigInteger
}
