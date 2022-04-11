package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.novafoundation.nova.core_db.model.PhishingSiteLocal

@Dao
abstract class PhishingSitesDao {

    @Query("SELECT EXISTS (SELECT * FROM phishing_sites WHERE host in (:hostSuffixes))")
    abstract suspend fun isPhishing(hostSuffixes: List<String>): Boolean

    @Transaction
    open suspend fun updatePhishingSites(newSites: List<PhishingSiteLocal>) {
        clearPhishingSites()

        insertPhishingSites(newSites)
    }

    @Query("DELETE FROM phishing_sites")
    protected abstract suspend fun clearPhishingSites()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPhishingSites(sites: List<PhishingSiteLocal>)
}
