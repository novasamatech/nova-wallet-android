package io.novafoundation.nova.runtime.multiNetwork.runtime.repository

import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal

interface RuntimeVersionsRepository {

    suspend fun getAllRuntimeVersions(): List<RuntimeVersion>
}

internal class DbRuntimeVersionsRepository(
    private val chainDao: ChainDao
) : RuntimeVersionsRepository {

    override suspend fun getAllRuntimeVersions(): List<RuntimeVersion> {
        return chainDao.allRuntimeInfos().map(::mapRuntimeInfoLocalToRuntimeVersion)
    }

    private fun mapRuntimeInfoLocalToRuntimeVersion(runtimeInfoLocal: ChainRuntimeInfoLocal): RuntimeVersion {
        return RuntimeVersion(
            chainId = runtimeInfoLocal.chainId,
            specVersion = runtimeInfoLocal.syncedVersion
        )
    }
}
