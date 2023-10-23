package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.NftLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NftDao {

    @Query("SELECT * FROM nfts WHERE metaId = :metaId")
    abstract fun nftsFlow(metaId: Long): Flow<List<NftLocal>>

    @Query("SELECT * FROM nfts WHERE metaId = :metaId AND type = :type")
    abstract suspend fun getNfts(metaId: Long, type: NftLocal.Type): List<NftLocal>

    @Delete
    protected abstract suspend fun deleteNfts(nfts: List<NftLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertNfts(nfts: List<NftLocal>)

    @Update
    protected abstract suspend fun updateNft(nft: NftLocal)

    @Query("SELECT * FROM nfts WHERE identifier = :nftIdentifier")
    abstract suspend fun getNft(nftIdentifier: String): NftLocal

    @Query("SELECT * FROM nfts WHERE identifier in (:nftIdentifiers)")
    abstract suspend fun getNfts(nftIdentifiers: List<String>): List<NftLocal>

    @Query("SELECT type FROM nfts WHERE identifier = :nftIdentifier")
    abstract suspend fun getNftType(nftIdentifier: String): NftLocal.Type

    @Query("UPDATE nfts SET wholeDetailsLoaded = 1 WHERE identifier = :nftIdentifier")
    abstract suspend fun markFullSynced(nftIdentifier: String)

    @Transaction
    open suspend fun insertNftsDiff(
        nftType: NftLocal.Type,
        metaId: Long,
        newNfts: List<NftLocal>,
        forceOverwrite: Boolean
    ) {
        val oldNfts = getNfts(metaId, nftType)

        val diff = CollectionDiffer.findDiff(newNfts, oldNfts, forceUseNewItems = forceOverwrite)

        deleteNfts(diff.removed)
        insertNfts(diff.newOrUpdated)
    }

    @Transaction
    open suspend fun updateNft(nftIdentifier: String, update: (NftLocal) -> NftLocal) {
        val nft = getNft(nftIdentifier)

        val updated = update(nft)

        updateNft(updated)
    }
}
