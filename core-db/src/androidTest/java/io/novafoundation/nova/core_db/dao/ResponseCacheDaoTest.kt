package io.novafoundation.nova.core_db.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.core_db.AppDatabase
import io.novafoundation.nova.core_db.model.ResponseCacheLocal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResponseCacheDaoTest : DaoTest<ResponseCacheDao>(AppDatabase::responseCacheDao) {

    private val accounts by dao<MetaAccountDao>()

    private val url = "https://infra.test/v1/bittensor/operations/search?accountSubject=shared-address&page=1"

    @Test
    fun identicalUrlsAreIsolatedByOwnerAndPublicRowsAreSeparate() = runBlocking {
        val first = accounts.insertMetaAccount(testMetaAccount())
        val second = accounts.insertMetaAccount(testMetaAccount())
        dao.put(ResponseCacheLocal(url, first, "first", 1, 2))
        dao.put(ResponseCacheLocal(url, second, "second", 1, 2))
        assertNull(dao.get(url, null))
        dao.put(ResponseCacheLocal(url, null, "public", 1, 2))
        assertEquals("first", dao.get(url, first)?.payload)
        assertEquals("second", dao.get(url, second)?.payload)
        assertEquals("public", dao.get(url, null)?.payload)
    }

    @Test
    fun removingOneWalletCascadesOnlyItsRowsIncludingSnapshots() = runBlocking {
        val first = accounts.insertMetaAccount(testMetaAccount())
        val second = accounts.insertMetaAccount(testMetaAccount())
        val snapshot = "bittensor://runtime/positions/chain?coldkey=hash"
        dao.put(ResponseCacheLocal(url, first, "first", 1, 2))
        dao.put(ResponseCacheLocal(snapshot, first, "positions", 1, 0))
        dao.put(ResponseCacheLocal(url, second, "second", 1, 2))
        dao.put(ResponseCacheLocal(url, null, "public", 1, 2))
        accounts.deleteByIds(listOf(first))
        assertNull(dao.get(url, first))
        assertNull(dao.get(snapshot, first))
        assertEquals("second", dao.get(url, second)?.payload)
        assertEquals("public", dao.get(url, null)?.payload)
    }

    @Test
    fun deletedOwnerCannotBeRepopulatedByALateWrite() = runBlocking {
        val owner = accounts.insertMetaAccount(testMetaAccount())
        accounts.deleteByIds(listOf(owner))
        val error = runCatching { dao.put(ResponseCacheLocal(url, owner, "late response", 1, 2)) }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
        assertNull(dao.get(url, owner))
    }
}
