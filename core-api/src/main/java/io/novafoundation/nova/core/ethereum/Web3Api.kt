package io.novafoundation.nova.core.ethereum

import io.novafoundation.nova.core.ethereum.log.Topic
import kotlinx.coroutines.flow.Flow
import org.web3j.protocol.Web3j
import org.web3j.protocol.websocket.events.LogNotification

interface Web3Api : Web3j {

    fun logsNotifications(addresses: List<String>, topics: List<Topic>): Flow<LogNotification>
}
