package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.PhishingSitesDao
import io.novafoundation.nova.core_db.model.PhishingSiteLocal
import io.novafoundation.nova.feature_dapp_impl.data.network.phishing.PhishingSitesApi
import io.novafoundation.nova.feature_dapp_impl.data.phisning.PhishingDetectingService

interface PhishingSitesRepository {

    suspend fun syncPhishingSites()

    suspend fun isPhishing(url: String): Boolean
}

class PhishingSitesRepositoryImpl(
    private val phishingSitesDao: PhishingSitesDao,
    private val phishingSitesApi: PhishingSitesApi,
    private val phishingDetectingService: PhishingDetectingService
) : PhishingSitesRepository {

    override suspend fun syncPhishingSites() {
        val remotePhishingSites = retryUntilDone { phishingSitesApi.getPhishingSites() }
        val toInsert = remotePhishingSites.deny.map(::PhishingSiteLocal)

        phishingSitesDao.updatePhishingSites(toInsert)
    }

    override suspend fun isPhishing(url: String): Boolean {
        return phishingDetectingService.isPhishing(url)
    }
}
