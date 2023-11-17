package io.novafoundation.nova.core_db.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.AppDatabase
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChainDaoTest : DaoTest<ChainDao>(AppDatabase::chainDao) {

    @Test
    fun shouldInsertWholeChain() = runBlocking {
        val chainInfo = createTestChain("0x00")

        dao.addChain(chainInfo)

        val chainsFromDb = dao.getJoinChainInfo()

        assertEquals(1, chainsFromDb.size)

        val chainFromDb = chainsFromDb.first()

        assertEquals(chainInfo.assets.size, chainFromDb.assets.size)
        assertEquals(chainInfo.nodes.size, chainFromDb.nodes.size)
    }

    @Test
    fun shouldDeleteChainWithCascade() = runBlocking {
        val chainInfo = createTestChain("0x00")

        dao.addChain(chainInfo)
        dao.removeChain(chainInfo)

        val assetsCursor = db.query("SELECT * FROM chain_assets", emptyArray())
        assertEquals(0, assetsCursor.count)

        val nodesCursor = db.query("SELECT * FROM chain_nodes", emptyArray())
        assertEquals(0, nodesCursor.count)
    }

    @Test
    fun shouldNotDeleteRuntimeCacheEntryAfterChainUpdate() = runBlocking {
        val chainInfo = createTestChain("0x00")

        dao.addChain(chainInfo)
        dao.updateRemoteRuntimeVersionIfChainExists(chainInfo.chain.id, runtimeVersion = 1, transactionVersion = 1)

        dao.updateChain(chainInfo)

        val runtimeEntry = dao.runtimeInfo(chainInfo.chain.id)

        assertNotNull(runtimeEntry)
    }

    @Test
    fun shouldDeleteRemovedNestedFields() = runBlocking {
        val chainInfo = createTestChain("0x00", nodesCount = 3, assetsCount = 3)

        dao.addChain(chainInfo)

        dao.applyDiff(
            chainDiff = updatedDiff(chainInfo.chain),
            assetsDiff = CollectionDiffer.Diff(
                added = emptyList(),
                updated = emptyList(),
                removed = chainInfo.assets.takeLast(1)
            ),
            nodesDiff = CollectionDiffer.Diff(
                added = emptyList(),
                updated = emptyList(),
                removed = chainInfo.nodes.takeLast(1)
            ),
            explorersDiff = emptyDiff()
        )

        val chainFromDb2 = dao.getJoinChainInfo().first()

        assertEquals(2, chainFromDb2.nodes.size)
        assertEquals(2, chainFromDb2.assets.size)
    }

    @Test
    fun shouldUpdate() = runBlocking {
        val toBeRemoved = listOf(
            createTestChain("to be removed 1"),
            createTestChain("to be removed 2"),
        )

        val stayTheSame = listOf(
            createTestChain("stay the same")
        )

        val chainsInitial = listOf(createTestChain("to be changed")) + stayTheSame + toBeRemoved

        dao.addChains(chainsInitial)

        val added = listOf(createTestChain("to be added"))
        val updated = listOf(createTestChain("to be changed", "new name"))

        val expectedResult = stayTheSame + added + updated

        dao.applyDiff(
            chainDiff = CollectionDiffer.Diff(
                added = added.map(JoinedChainInfo::chain),
                updated = updated.map(JoinedChainInfo::chain),
                removed = toBeRemoved.map(JoinedChainInfo::chain)
            ),
            assetsDiff = emptyDiff(),
            nodesDiff = emptyDiff(),
            explorersDiff = emptyDiff()
        )

        val chainsFromDb = dao.getJoinChainInfo()

        assertEquals(expectedResult.size, chainsFromDb.size)
        expectedResult.forEach { expected ->
            val tryFind = chainsFromDb.firstOrNull { actual -> expected.chain.id == actual.chain.id && expected.chain.name == actual.chain.name }

            assertNotNull("Did not find ${expected.chain.id} in result set", tryFind)
        }
    }

    @Test
    fun shouldUpdateRuntimeVersions() {
        runBlocking {
            val chainId = "0x00"

            dao.addChain(createTestChain(chainId))

            dao.updateRemoteRuntimeVersionIfChainExists(chainId, 1)

            checkRuntimeVersions(remote = 1, synced = 0)

            dao.updateSyncedRuntimeVersion(chainId, 1)

            checkRuntimeVersions(remote = 1, synced = 1)

            dao.updateRemoteRuntimeVersionIfChainExists(chainId, 2)

            checkRuntimeVersions(remote = 2, synced = 1)
        }
    }

    private suspend fun checkRuntimeVersions(remote: Int, synced: Int) {
        val runtimeInfo = dao.runtimeInfo("0x00")

        requireNotNull(runtimeInfo)

        assertEquals(runtimeInfo.remoteVersion, remote)
        assertEquals(runtimeInfo.syncedVersion, synced)
    }
}
