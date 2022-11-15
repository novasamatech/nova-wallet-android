package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evm

import io.novafoundation.nova.runtime.ethereum.contract.base.queryBatched
import io.novafoundation.nova.runtime.ethereum.contract.base.querySingle
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Queries
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.updater.EthereumSharedRequestsBuilder
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireErc20
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.ethereumApi
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import java.math.BigInteger

private const val BATCH_ID = "EvmAssetBalance.InitialBalance"

class EvmAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val erc20Standard: Erc20Standard,
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

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return false
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        // ERC20 tokens do not have ED
        return BigInteger.ZERO
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val erc20Type = chainAsset.requireErc20()
        val ethereumApi = chainRegistry.ethereumApi(chain.id)
        val accountAddress = chain.addressOf(accountId)

        return erc20Standard.querySingle(erc20Type.contractAddress, ethereumApi)
            .balanceOfAsync(accountAddress)
            .await()
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
        val ethereumApi = chainRegistry.ethereumApi(chain.id)

        val initialBalanceAsync = erc20Standard.queryBatched(erc20Type.contractAddress, BATCH_ID, subscriptionBuilder)
            .balanceOfAsync(address)

        return subscriptionBuilder.erc20BalanceFlow(address, ethereumApi, chainAsset, initialBalanceAsync)
            .map { balanceUpdate ->
                updateAsset(metaAccount.id, chainAsset, balanceUpdate.newBalance)

                if (balanceUpdate.cause != null) {
                    BalanceSyncUpdate.CauseFetched(balanceUpdate.cause)
                } else {
                    BalanceSyncUpdate.NoCause
                }
            }
    }

    private suspend fun updateAsset(
        metaId: Long,
        chainAsset: Chain.Asset,
        newBalance: Balance
    ) {
        assetCache.updateAsset(metaId, chainAsset) {
            it.copy(freeInPlanks = newBalance)
        }
    }

    private fun EthereumSharedRequestsBuilder.erc20BalanceFlow(
        account: String,
        web3Api: Web3Api,
        chainAsset: Chain.Asset,
        initialBalanceAsync: Deferred<BigInteger>
    ): Flow<Erc20BalanceUpdate> {
        val contractAddress = chainAsset.requireErc20().contractAddress

        val changes = accountErcTransfersFlow(account, contractAddress, chainAsset).map { erc20Transfer ->
            val newBalance = erc20Standard.querySingle(contractAddress, web3Api)
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

    private fun EthereumSharedRequestsBuilder.accountErcTransfersFlow(
        accountAddress: String,
        contractAddress: String,
        chainAsset: Chain.Asset,
    ): Flow<TransferExtrinsic> {
        val addressTopic = TypeEncoder.encode(Address(accountAddress))

        val transferEvent = Erc20Queries.TRANSFER_EVENT
        val transferEventSignature = EventEncoder.encode(transferEvent)

        val erc20SendTopic = listOf(
            Topic.Single(transferEventSignature), // zero-th topic is event signature
            Topic.AnyOf(addressTopic), // our account as `from`
        )

        val erc20ReceiveTopic = listOf(
            Topic.Single(transferEventSignature), // zero-th topic is event signature
            Topic.Any, // anyone is `from`
            Topic.AnyOf(addressTopic) // out account as `to`
        )

        val receiveTransferNotifications = subscribeEthLogs(contractAddress, erc20ReceiveTopic)
        val sendTransferNotifications = subscribeEthLogs(contractAddress, erc20SendTopic)

        val transferNotifications = merge(receiveTransferNotifications, sendTransferNotifications)

        return transferNotifications.map { logNotification ->
            val log = logNotification.params.result
            val event = Erc20Queries.parseTransferEvent(log)

            TransferExtrinsic(
                senderId = event.from.accountId(),
                recipientId = event.to.accountId(),
                amountInPlanks = event.amount.value,
                chainAsset = chainAsset,
                status = ExtrinsicStatus.SUCCESS,
                hash = log.transactionHash,
            )
        }
    }
}

private fun Address.accountId() = value.asEthereumAddress().toAccountId().value

private class Erc20BalanceUpdate(
    val newBalance: Balance,
    val cause: TransferExtrinsic?
)
