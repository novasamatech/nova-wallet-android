package io.novafoundation.nova.feature_dapp_impl.data.phisning

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.core_db.dao.PhishingSitesDao

interface PhishingDetectingService {

    suspend fun isPhishing(url: String): Boolean
}

class CompoundPhishingDetectingService(
    private val services: List<PhishingDetectingService>
) : PhishingDetectingService {

    override suspend fun isPhishing(url: String): Boolean {
        return services.any { it.isPhishing(url) }
    }
}

class BlackListPhishingDetectingService(
    private val phishingSitesDao: PhishingSitesDao,
) : PhishingDetectingService {

    override suspend fun isPhishing(url: String): Boolean {
        val host = Urls.hostOf(url)
        val hostSuffixes = extractAllPossibleSubDomains(host)

        return phishingSitesDao.isPhishing(hostSuffixes)
    }

    private fun extractAllPossibleSubDomains(host: String): List<String> {
        val separator = "."

        val segments = host.split(separator)

        val suffixes = (2..segments.size).map { suffixLength ->
            segments.takeLast(suffixLength).joinToString(separator = ".")
        }

        return suffixes
    }
}

class DomainListPhishingDetectingService(
    private val blackListDomains: List<String> // top
) : PhishingDetectingService {

    override suspend fun isPhishing(url: String): Boolean {
        val host = Urls.hostOf(url)
        val urlTopLevelDomain = extractTopLevelDomain(host)

        return blackListDomains.any { urlTopLevelDomain == it }
    }

    private fun extractTopLevelDomain(host: String): String {
        val separator = "."

        val segments = host.split(separator)

        return segments.last()
    }
}
