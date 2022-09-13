package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChainDao {

    @Transaction
    open suspend fun applyDiff(
        chainDiff: CollectionDiffer.Diff<ChainLocal>,
        assetsDiff: CollectionDiffer.Diff<ChainAssetLocal>,
        nodesDiff: CollectionDiffer.Diff<ChainNodeLocal>,
        explorersDiff: CollectionDiffer.Diff<ChainExplorerLocal>
    ) {
        deleteChains(chainDiff.removed)
        deleteChainAssets(assetsDiff.removed)
        deleteChainNodes(nodesDiff.removed)
        deleteChainExplorers(explorersDiff.removed)

        addChains(chainDiff.added)
        addChainAssets(assetsDiff.added)
        addChainNodes(nodesDiff.added)
        addChainExplorers(explorersDiff.added)

        updateChains(chainDiff.updated)
        updateChainAssets(assetsDiff.updated)
        updateChainNodes(nodesDiff.updated)
        updateChainExplorers(explorersDiff.updated)
    }

    // ------ Delete --------
    @Delete
    protected abstract suspend fun deleteChains(chains: List<ChainLocal>)

    @Delete
    protected abstract suspend fun deleteChainNodes(nodes: List<ChainNodeLocal>)

    @Delete
    protected abstract suspend fun deleteChainAssets(assets: List<ChainAssetLocal>)

    @Delete
    protected abstract suspend fun deleteChainExplorers(explorers: List<ChainExplorerLocal>)

    // ------ Add --------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChains(chains: List<ChainLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChainNodes(nodes: List<ChainNodeLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChainAssets(assets: List<ChainAssetLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChainExplorers(explorers: List<ChainExplorerLocal>)

    // ------ Update -----

    @Update
    protected abstract suspend fun updateChains(chains: List<ChainLocal>)

    @Update
    protected abstract suspend fun updateChainNodes(nodes: List<ChainNodeLocal>)

    @Update
    protected abstract suspend fun updateChainAssets(assets: List<ChainAssetLocal>)

    @Update
    protected abstract suspend fun updateChainExplorers(explorers: List<ChainExplorerLocal>)

    @Query("SELECT * FROM chains")
    @Transaction
    abstract suspend fun getJoinChainInfo(): List<JoinedChainInfo>

    @Query("SELECT * FROM chains")
    @Transaction
    abstract fun joinChainInfoFlow(): Flow<List<JoinedChainInfo>>

    @Query("SELECT EXISTS(SELECT * FROM chains WHERE id = :chainId)")
    abstract suspend fun chainExists(chainId: String): Boolean

    @Query("SELECT * FROM chain_runtimes WHERE chainId = :chainId")
    abstract suspend fun runtimeInfo(chainId: String): ChainRuntimeInfoLocal?

    @Query("SELECT * FROM chain_runtimes")
    abstract suspend fun allRuntimeInfos(): List<ChainRuntimeInfoLocal>

    @Query("UPDATE chain_runtimes SET syncedVersion = :syncedVersion WHERE chainId = :chainId")
    abstract suspend fun updateSyncedRuntimeVersion(chainId: String, syncedVersion: Int)

    @Transaction
    open suspend fun updateRemoteRuntimeVersionIfChainExists(chainId: String, remoteVersion: Int) {
        if (!chainExists(chainId)) return

        if (isRuntimeInfoExists(chainId)) {
            updateRemoteRuntimeVersionUnsafe(chainId, remoteVersion)
        } else {
            insertRuntimeInfo(ChainRuntimeInfoLocal(chainId, syncedVersion = 0, remoteVersion = remoteVersion))
        }
    }

    @Query("UPDATE chain_runtimes SET remoteVersion = :remoteVersion WHERE chainId = :chainId")
    protected abstract suspend fun updateRemoteRuntimeVersionUnsafe(chainId: String, remoteVersion: Int)

    @Query("SELECT EXISTS (SELECT * FROM chain_runtimes WHERE chainId = :chainId)")
    protected abstract suspend fun isRuntimeInfoExists(chainId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRuntimeInfo(runtimeInfoLocal: ChainRuntimeInfoLocal)
}
