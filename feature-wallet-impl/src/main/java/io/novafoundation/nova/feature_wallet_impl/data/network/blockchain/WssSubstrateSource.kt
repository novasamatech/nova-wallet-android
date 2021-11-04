@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.ExtrinsicStatusEvent
import io.novafoundation.nova.common.data.network.runtime.binding.Phase
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindEventRecords
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.bindings.bindTransferExtrinsic
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import java.math.BigInteger

class WssSubstrateSource(
    private val rpcCalls: RpcCalls,
    private val remoteStorageSource: StorageDataSource,
    private val extrinsicService: ExtrinsicService,
) : SubstrateRemoteSource {

    override suspend fun getAccountInfo(
        chainId: ChainId,
        accountId: AccountId,
    ): AccountInfo {
        return remoteStorageSource.query(
            chainId = chainId,
            keyBuilder = {
                it.metadata.system().storage("Account").storageKey(it, accountId)
            },
            binding = { scale, runtime ->
                scale?.let { bindAccountInfo(it, runtime) } ?: AccountInfo.empty()
            }
        )
    }

    override suspend fun getTransferFee(chain: Chain, transfer: Transfer): BigInteger {
        return extrinsicService.estimateFee(chain) {
            transfer(chain, transfer)
        }
    }

    override suspend fun performTransfer(
        accountId: AccountId,
        chain: Chain,
        transfer: Transfer,
    ): String {
        return extrinsicService.submitExtrinsic(chain, accountId) {
            transfer(chain, transfer)
        }.getOrThrow()
    }

    override suspend fun fetchAccountTransfersInBlock(
        chainId: ChainId,
        blockHash: String,
        accountId: ByteArray,
    ): Result<List<TransferExtrinsicWithStatus>> = runCatching {
        val block = rpcCalls.getBlock(chainId, blockHash)

        val extrinsics = remoteStorageSource.queryNonNull(
            chainId = chainId,
            keyBuilder = { it.metadata.system().storage("Events").storageKey() },
            binding = { scale, runtime ->
                val statuses = bindEventRecords(scale, runtime)
                    .mapNotNull {
                        val extrinsicStatus = it.event.asExtrinsicStatus()
                        val phase = it.phase as? Phase.ApplyExtrinsic

                        if (extrinsicStatus != null && phase != null) {
                            phase.extrinsicId.toInt() to extrinsicStatus
                        } else {
                            null
                        }
                    }
                    .associateBy(
                        keySelector = Pair<Int, ExtrinsicStatusEvent>::first,
                        valueTransform = Pair<Int, ExtrinsicStatusEvent>::second
                    )

                buildExtrinsics(runtime, statuses, block.block.extrinsics)
            },
            at = blockHash
        )

        extrinsics.filter { transferWithStatus ->
            val extrinsic = transferWithStatus.extrinsic

            extrinsic.senderId.contentEquals(accountId) || extrinsic.recipientId.contentEquals(accountId)
        }
    }

    private fun GenericEvent.Instance.asExtrinsicStatus(): ExtrinsicStatusEvent? {
        return if (module.name == Modules.SYSTEM) {
            when (event.name) {
                "ExtrinsicFailed" -> ExtrinsicStatusEvent.FAILURE
                "ExtrinsicSuccess" -> ExtrinsicStatusEvent.SUCCESS
                else -> null
            }
        } else null
    }

    private fun buildExtrinsics(
        runtime: RuntimeSnapshot,
        statuses: Map<Int, ExtrinsicStatusEvent>,
        extrinsicsRaw: List<String>,
    ): List<TransferExtrinsicWithStatus> {
        return extrinsicsRaw.mapIndexed { index, extrinsicScale ->
            val transferExtrinsic = bindOrNull { bindTransferExtrinsic(extrinsicScale, runtime) }

            transferExtrinsic?.let {
                TransferExtrinsicWithStatus(transferExtrinsic, statuses[index])
            }
        }.filterNotNull()
    }

    private fun ExtrinsicBuilder.transfer(chain: Chain, transfer: Transfer): ExtrinsicBuilder {
        return transfer(
            accountId = chain.accountIdOf(transfer.recipient),
            amount = transfer.amountInPlanks
        )
    }
}
