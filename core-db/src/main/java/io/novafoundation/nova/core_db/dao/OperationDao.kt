package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.core_db.model.operation.DirectRewardTypeLocal
import io.novafoundation.nova.core_db.model.operation.ExtrinsicTypeLocal
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.OperationJoin
import io.novafoundation.nova.core_db.model.operation.OperationLocal
import io.novafoundation.nova.core_db.model.operation.OperationTypeLocal
import io.novafoundation.nova.core_db.model.operation.PoolRewardTypeLocal
import io.novafoundation.nova.core_db.model.operation.SwapTypeLocal
import io.novafoundation.nova.core_db.model.operation.TransferTypeLocal
import kotlinx.coroutines.flow.Flow

private const val ID_FILTER = "address = :address AND chainId = :chainId AND assetId = :chainAssetId"

@Dao
abstract class OperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOperationBase(operation: OperationBaseLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransferType(type: TransferTypeLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDirectRewardType(type: DirectRewardTypeLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPoolRewardType(type: PoolRewardTypeLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExtrinsicType(type: ExtrinsicTypeLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSwapType(type: SwapTypeLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOperationsBase(operations: List<OperationBaseLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransferTypes(types: List<TransferTypeLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDirectRewardTypes(types: List<DirectRewardTypeLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPoolRewardTypes(types: List<PoolRewardTypeLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExtrinsicTypes(types: List<ExtrinsicTypeLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSwapTypes(types: List<SwapTypeLocal>)

    @Transaction
    open suspend fun insert(operation: OperationLocal) {
        insertOperationBase(operation.base)
        insertOperationType(operation.type)
    }

    @Transaction
    open suspend fun insertAll(operations: List<OperationLocal>) {
        insertAllInternal(operations)
    }

    @Query(
        """
        SELECT 
        o.assetId as o_assetId, o.chainId o_chainId, o.id as o_id, o.address as o_address, o.time o_time, o.status as o_status, o.source o_source, o.hash as o_hash,
        t.amount as t_amount, t.fee as t_fee, t.sender as t_sender, t.receiver as t_receiver,
        e.contentType as e_contentType, e.module as e_module, e.call as e_call, e.fee as e_fee,
        rd.isReward as rd_isReward, rd.amount as rd_amount, rd.validator as rd_validator, rd.era as rd_era, rd.eventId as rd_eventId,
        rp.isReward as rp_isReward, rp.amount as rp_amount, rp.poolId as rp_poolId, rp.eventId as rp_eventId,
        s.fee_chainId as s_fee_chainId, s.fee_assetId as s_fee_assetId, s.fee_amount as s_fee_amount,
        s.assetIn_chainId as s_assetIn_chainId, s.assetIn_assetId as s_assetIn_assetId, s.assetIn_amount as s_assetIn_amount,
        s.assetOut_chainId as s_assetOut_chainId, s.assetOut_assetId as s_assetOut_assetId, s.assetOut_amount as s_assetOut_amount
        FROM operations as o
        LEFT JOIN operation_transfers as t ON t.operationId = o.id AND t.assetId = o.assetId AND t.chainId = o.chainId AND t.address = o.address
        LEFT JOIN operation_extrinsics as e ON e.operationId = o.id AND e.assetId = o.assetId AND e.chainId = o.chainId AND e.address = o.address
        LEFT JOIN operation_rewards_direct as rd ON rd.operationId = o.id AND rd.assetId = o.assetId AND rd.chainId = o.chainId AND rd.address = o.address
        LEFT JOIN operation_rewards_pool as rp ON rp.operationId = o.id AND rp.assetId = o.assetId AND rp.chainId = o.chainId AND rp.address = o.address
        LEFT JOIN operation_swaps as s ON s.operationId = o.id AND s.assetId = o.assetId AND s.chainId = o.chainId AND s.address = o.address
        WHERE o.address = :address AND o.chainId = :chainId AND o.assetId = :chainAssetId
        ORDER BY (case when o.status = :statusUp then 0 else 1 end), o.time DESC
        """
    )
    abstract fun observe(
        address: String,
        chainId: String,
        chainAssetId: Int,
        statusUp: OperationBaseLocal.Status = OperationBaseLocal.Status.PENDING
    ): Flow<List<OperationJoin>>

    @Query(
        """
        SELECT * FROM operation_transfers
        WHERE address = :address AND chainId = :chainId AND assetId = :chainAssetId AND operationId = :operationId
        """
    )
    abstract suspend fun getTransferType(
        operationId: String,
        address: String,
        chainId: String,
        chainAssetId: Int
    ): TransferTypeLocal?

    @Transaction
    open suspend fun insertFromRemote(
        accountAddress: String,
        chainId: String,
        chainAssetId: Int,
        operations: List<OperationLocal>
    ) {
        clearBySource(accountAddress, chainId, chainAssetId, OperationBaseLocal.Source.REMOTE)

        val operationsWithHashes = operations.mapNotNullToSet { it.base.hash }
        if (operationsWithHashes.isNotEmpty()) {
            clearByHashes(accountAddress, chainId, chainAssetId, operationsWithHashes)
        }

        val oldestTime = operations.minOfOrNull { it.base.time }
        oldestTime?.let {
            clearOld(accountAddress, chainId, chainAssetId, oldestTime)
        }

        insertAllInternal(operations)
    }

    @Query("DELETE FROM operations WHERE $ID_FILTER AND source = :source")
    protected abstract suspend fun clearBySource(
        address: String,
        chainId: String,
        chainAssetId: Int,
        source: OperationBaseLocal.Source
    ): Int

    @Query("DELETE FROM operations WHERE time < :minTime AND $ID_FILTER")
    protected abstract suspend fun clearOld(
        address: String,
        chainId: String,
        chainAssetId: Int,
        minTime: Long
    ): Int

    @Query("DELETE FROM operations WHERE $ID_FILTER AND hash in (:hashes)")
    protected abstract suspend fun clearByHashes(
        address: String,
        chainId: String,
        chainAssetId: Int,
        hashes: Set<String>
    ): Int

    private suspend fun insertOperationType(type: OperationTypeLocal) {
        when (type) {
            is ExtrinsicTypeLocal -> insertExtrinsicType(type)
            is DirectRewardTypeLocal -> insertDirectRewardType(type)
            is PoolRewardTypeLocal -> insertPoolRewardType(type)
            is SwapTypeLocal -> insertSwapType(type)
            is TransferTypeLocal -> insertTransferType(type)
            else -> {}
        }
    }

    private suspend fun insertAllInternal(operations: List<OperationLocal>) {
        insertOperationsBase(operations.map { it.base })
        insertOperationTypes(operations.map { it.type })
    }

    private suspend fun insertOperationTypes(types: List<OperationTypeLocal>) {
        val transfers = types.filterIsInstance<TransferTypeLocal>()
        val extrinsics = types.filterIsInstance<ExtrinsicTypeLocal>()
        val directRewards = types.filterIsInstance<DirectRewardTypeLocal>()
        val poolRewards = types.filterIsInstance<PoolRewardTypeLocal>()
        val swaps = types.filterIsInstance<SwapTypeLocal>()

        insertTransferTypes(transfers)
        insertExtrinsicTypes(extrinsics)
        insertDirectRewardTypes(directRewards)
        insertPoolRewardTypes(poolRewards)
        insertSwapTypes(swaps)
    }
}
