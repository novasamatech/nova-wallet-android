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
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChainDao {

    @Transaction
    open suspend fun applyDiff(
        chainDiff: CollectionDiffer.Diff<ChainLocal>,
        assetsDiff: CollectionDiffer.Diff<ChainAssetLocal>,
        nodesDiff: CollectionDiffer.Diff<ChainNodeLocal>,
        explorersDiff: CollectionDiffer.Diff<ChainExplorerLocal>,
        externalApisDiff: CollectionDiffer.Diff<ChainExternalApiLocal>,
        nodeSelectionPreferencesDiff: CollectionDiffer.Diff<NodeSelectionPreferencesLocal>
    ) {
        deleteChains(chainDiff.removed)
        deleteChainAssets(assetsDiff.removed)
        deleteChainNodes(nodesDiff.removed)
        deleteChainExplorers(explorersDiff.removed)
        deleteExternalApis(externalApisDiff.removed)
        deleteNodePreferences(nodeSelectionPreferencesDiff.removed)

        addChains(chainDiff.added)
        addChainAssets(assetsDiff.added)
        addChainNodes(nodesDiff.added)
        addChainExplorers(explorersDiff.added)
        addExternalApis(externalApisDiff.added)
        addNodePreferences(nodeSelectionPreferencesDiff.added)

        updateChains(chainDiff.updated)
        updateChainAssets(assetsDiff.updated)
        updateChainNodes(nodesDiff.updated)
        updateChainExplorers(explorersDiff.updated)
        updateExternalApis(externalApisDiff.updated)
        updateNodePreferences(nodeSelectionPreferencesDiff.added)
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

    @Delete
    protected abstract suspend fun deleteExternalApis(apis: List<ChainExternalApiLocal>)

    @Delete
    protected abstract suspend fun deleteNodePreferences(apis: List<NodeSelectionPreferencesLocal>)

    // ------ Add --------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChains(chains: List<ChainLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChainNodes(nodes: List<ChainNodeLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChainAssets(assets: List<ChainAssetLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addChainExplorers(explorers: List<ChainExplorerLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addExternalApis(apis: List<ChainExternalApiLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun addNodePreferences(model: List<NodeSelectionPreferencesLocal>)

    // ------ Update -----

    @Update
    protected abstract suspend fun updateChains(chains: List<ChainLocal>)

    @Update
    protected abstract suspend fun updateChainNodes(nodes: List<ChainNodeLocal>)

    @Update
    protected abstract suspend fun updateChainAssets(assets: List<ChainAssetLocal>)

    @Update
    protected abstract suspend fun updateChainExplorers(explorers: List<ChainExplorerLocal>)

    @Update
    protected abstract suspend fun updateExternalApis(apis: List<ChainExternalApiLocal>)

    @Update
    protected abstract suspend fun updateNodePreferences(apis: List<NodeSelectionPreferencesLocal>)

    // ------- Queries ------

    @Query("SELECT * FROM chains")
    @Transaction
    abstract suspend fun getJoinChainInfo(): List<JoinedChainInfo>

    @Query("SELECT id FROM chains")
    @Transaction
    abstract suspend fun getAllChainIds(): List<String>

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

    @Query("UPDATE chains SET connectionState = :connectionState WHERE id = :chainId")
    abstract suspend fun setConnectionState(chainId: String, connectionState: ChainLocal.ConnectionStateLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun setNodePreferences(model: NodeSelectionPreferencesLocal)

    @Transaction
    open suspend fun updateRemoteRuntimeVersionIfChainExists(chainId: String, runtimeVersion: Int, transactionVersion: Int) {
        if (!chainExists(chainId)) return

        if (isRuntimeInfoExists(chainId)) {
            updateRemoteRuntimeVersionUnsafe(chainId, runtimeVersion, transactionVersion)
        } else {
            insertRuntimeInfo(ChainRuntimeInfoLocal(chainId, syncedVersion = 0, remoteVersion = runtimeVersion, transactionVersion = transactionVersion))
        }
    }

    @Query("UPDATE chain_runtimes SET remoteVersion = :remoteVersion, transactionVersion = :transactionVersion WHERE chainId = :chainId")
    protected abstract suspend fun updateRemoteRuntimeVersionUnsafe(chainId: String, remoteVersion: Int, transactionVersion: Int)

    @Query("SELECT EXISTS (SELECT * FROM chain_runtimes WHERE chainId = :chainId)")
    protected abstract suspend fun isRuntimeInfoExists(chainId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRuntimeInfo(runtimeInfoLocal: ChainRuntimeInfoLocal)
}
