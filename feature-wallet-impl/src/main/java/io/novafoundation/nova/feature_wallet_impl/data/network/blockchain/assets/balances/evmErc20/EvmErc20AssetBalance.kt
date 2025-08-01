package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evmErc20

import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.updater.EthereumSharedRequestsBuilder
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.callApiOrThrow
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.updateNonLockableAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdatePoint
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.ethereum.contract.base.queryBatched
import io.novafoundation.nova.runtime.ethereum.contract.base.querySingle
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Queries
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireErc20
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.getSubscriptionEthereumApiOrThrow
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.protocol.websocket.events.Log
import org.web3j.protocol.websocket.events.LogNotification
import java.math.BigInteger

private const val BATCH_ID = "EvmAssetBalance.InitialBalance"

class EvmErc20AssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val erc20Standard: Erc20Standard,
    private val rpcCalls: RpcCalls,
) : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        // ERC20 tokens doe not support locks
        return emptyFlow<Nothing>()
    }

    override fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chainAsset: Chain.Asset): BigInteger {
        // ERC20 tokens do not have ED
        return BigInteger.ZERO
    }

    override suspend fun queryAccountBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): ChainAssetBalance {
        val erc20Type = chainAsset.requireErc20()
        val ethereumApi = chainRegistry.getCallEthereumApiOrThrow(chain.id)
        val accountAddress = chain.addressOf(accountId)
        val balance = erc20Standard.querySingle(erc20Type.contractAddress, ethereumApi)
            .balanceOfAsync(accountAddress)
            .await()
        return ChainAssetBalance.fromFree(chainAsset, free = balance)
    }

    override suspend fun subscribeAccountBalanceUpdatePoint(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
    ): Flow<TransferableBalanceUpdatePoint> {
        val ethereumApi = chainRegistry.getSubscriptionEthereumApiOrThrow(chain.id)

        val address = chain.addressOf(accountId)
        val erc20Type = chainAsset.requireErc20()

        return merge(
            ethereumApi.incomingErcTransfersFlow(address, erc20Type.contractAddress),
            ethereumApi.outComingErcTransfersFlow(address, erc20Type.contractAddress)
        ).mapLatest { logNotification ->
            val blockNumber = logNotification.params.result.parsedBlockNumber()
            val substrateHash = rpcCalls.getBlockHash(chain.id, blockNumber)

            TransferableBalanceUpdatePoint(updatedAt = substrateHash)
        }
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        val address = chain.addressOf(accountId)

        val erc20Type = chainAsset.requireErc20()

        val initialBalanceAsync = erc20Standard.queryBatched(erc20Type.contractAddress, BATCH_ID, subscriptionBuilder)
            .balanceOfAsync(address)

        return subscriptionBuilder.erc20BalanceFlow(address, chainAsset, initialBalanceAsync)
            .map { balanceUpdate ->
                assetCache.updateNonLockableAsset(metaAccount.id, chainAsset, balanceUpdate.newBalance)

                if (balanceUpdate.cause != null) {
                    BalanceSyncUpdate.CauseFetched(balanceUpdate.cause)
                } else {
                    BalanceSyncUpdate.NoCause
                }
            }
    }

    private fun EthereumSharedRequestsBuilder.erc20BalanceFlow(
        account: String,
        chainAsset: Chain.Asset,
        initialBalanceAsync: Deferred<BigInteger>
    ): Flow<Erc20BalanceUpdate> {
        val contractAddress = chainAsset.requireErc20().contractAddress

        val changes = accountErcTransfersFlow(account, contractAddress, chainAsset).map { erc20Transfer ->
            val newBalance = erc20Standard.querySingle(contractAddress, callApiOrThrow)
                .balanceOfAsync(account)
                .await()

            Erc20BalanceUpdate(newBalance, cause = erc20Transfer)
        }

        return flow {
            val initialBalance = initialBalanceAsync.await()

            emit(Erc20BalanceUpdate(initialBalance, cause = null))

            emitAll(changes)
        }
    }

    private fun Web3Api.incomingErcTransfersFlow(
        accountAddress: String,
        contractAddress: String,
    ): Flow<LogNotification> {
        return logsNotifications(
            addresses = listOf(contractAddress),
            topics = createErc20ReceiveTopics(accountAddress)
        )
    }

    private fun Web3Api.outComingErcTransfersFlow(
        accountAddress: String,
        contractAddress: String,
    ): Flow<LogNotification> {
        return logsNotifications(
            addresses = listOf(contractAddress),
            topics = createErc20SendTopics(accountAddress)
        )
    }

    private fun createErc20ReceiveTopics(accountAddress: String): List<Topic> {
        val addressTopic = TypeEncoder.encode(Address(accountAddress))
        val transferEventSignature = EventEncoder.encode(Erc20Queries.TRANSFER_EVENT)

        return createErc20ReceiveTopics(transferEventSignature, addressTopic)
    }

    private fun createErc20ReceiveTopics(
        transferEventSignature: String,
        addressTopic: String,
    ): List<Topic> {
        return listOf(
            Topic.Single(transferEventSignature), // zero-th topic is event signature
            Topic.Any, // anyone is `from`
            Topic.AnyOf(addressTopic) // our account as `to`
        )
    }

    private fun createErc20SendTopics(accountAddress: String): List<Topic> {
        val addressTopic = TypeEncoder.encode(Address(accountAddress))
        val transferEventSignature = EventEncoder.encode(Erc20Queries.TRANSFER_EVENT)

        return createErc20SendTopics(transferEventSignature, addressTopic)
    }

    private fun createErc20SendTopics(
        transferEventSignature: String,
        addressTopic: String,
    ): List<Topic> {
        return listOf(
            Topic.Single(transferEventSignature), // zero-th topic is event signature
            Topic.AnyOf(addressTopic), // our account as `from`
        )
    }

    private fun EthereumSharedRequestsBuilder.accountErcTransfersFlow(
        accountAddress: String,
        contractAddress: String,
        chainAsset: Chain.Asset,
    ): Flow<RealtimeHistoryUpdate> {
        val addressTopic = TypeEncoder.encode(Address(accountAddress))

        val transferEvent = Erc20Queries.TRANSFER_EVENT
        val transferEventSignature = EventEncoder.encode(transferEvent)

        val erc20SendTopic = createErc20SendTopics(transferEventSignature, addressTopic)
        val erc20ReceiveTopic = createErc20ReceiveTopics(transferEventSignature, addressTopic)

        val receiveTransferNotifications = subscribeEthLogs(contractAddress, erc20ReceiveTopic)
        val sendTransferNotifications = subscribeEthLogs(contractAddress, erc20SendTopic)

        val transferNotifications = merge(receiveTransferNotifications, sendTransferNotifications)

        return transferNotifications.map { logNotification ->
            val log = logNotification.params.result
            val event = Erc20Queries.parseTransferEvent(log)

            RealtimeHistoryUpdate(
                status = Operation.Status.COMPLETED,
                txHash = log.transactionHash,
                type = RealtimeHistoryUpdate.Type.Transfer(
                    senderId = event.from.accountId(),
                    recipientId = event.to.accountId(),
                    amountInPlanks = event.amount.value,
                    chainAsset = chainAsset,
                )
            )
        }
    }

    private fun Log.parsedBlockNumber(): BigInteger {
        return BigInteger(blockNumber.removeHexPrefix(), 16)
    }
}

private fun Address.accountId() = value.asEthereumAddress().toAccountId().value

private class Erc20BalanceUpdate(
    val newBalance: Balance,
    val cause: RealtimeHistoryUpdate?
)
