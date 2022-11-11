package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evm

import io.novafoundation.nova.common.data.network.ethereum.contract.erc20.ReadOnlyErc20
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.updater.EthereumSubscriptionBuilder
import io.novafoundation.nova.core.updater.SubscriptionBuilder
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

class EvmAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
) : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<*> {
        // ERC20 tokens doe not support locks
        return emptyFlow<Nothing>()
    }

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return false
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        // ERC20 tokens do not have
        return BigInteger.ZERO
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val erc20Type = chainAsset.requireErc20()
        val ethereumApi = chainRegistry.ethereumApi(chain.id)
        val erc20 = ReadOnlyErc20(erc20Type.contractAddress, ethereumApi)
        val accountAddress = chain.addressOf(accountId)

        return erc20.balanceOf(accountAddress)
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BalanceSyncUpdate> {
        val address = chain.addressOf(accountId)

        val erc20Type = chainAsset.requireErc20()
        val ethereumApi = chainRegistry.ethereumApi(chain.id)
        val erc20 = ReadOnlyErc20(erc20Type.contractAddress, ethereumApi)

        return subscriptionBuilder.erc20BalanceFlow(address, erc20, chainAsset)
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

    private fun EthereumSubscriptionBuilder.erc20BalanceFlow(
        account: String,
        contract: ReadOnlyErc20,
        chainAsset: Chain.Asset,
    ): Flow<Erc20BalanceUpdate> {
        val changes = accountErcTransfersFlow(account, contract.address, chainAsset).map { erc20Transfer ->
            val newBalance = contract.balanceOf(account)

            Erc20BalanceUpdate(newBalance, cause = erc20Transfer)
        }

        return flow {
            val initialBalance = contract.balanceOf(account)

            emit(Erc20BalanceUpdate(initialBalance, cause = null))

            emitAll(changes)
        }
    }

    private fun EthereumSubscriptionBuilder.accountErcTransfersFlow(
        accountAddress: String,
        contractAddress: String,
        chainAsset: Chain.Asset,
    ): Flow<TransferExtrinsic> {
        val addressTopic = TypeEncoder.encode(Address(accountAddress))

        val transferEvent = ReadOnlyErc20.TRANSFER_EVENT
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
            val event = ReadOnlyErc20.parseTransferEvent(log)

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
