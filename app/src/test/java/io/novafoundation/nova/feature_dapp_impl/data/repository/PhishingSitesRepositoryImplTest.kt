package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.core_db.dao.PhishingSitesDao
import io.novafoundation.nova.feature_dapp_impl.data.network.phishing.PhishingSitesApi
import io.novafoundation.nova.feature_dapp_impl.data.phisning.BlackListPhishingDetectingService
import io.novafoundation.nova.feature_dapp_impl.data.phisning.CompoundPhishingDetectingService
import io.novafoundation.nova.feature_dapp_impl.data.phisning.DomainListPhishingDetectingService
import io.novafoundation.nova.feature_dapp_impl.data.phisning.PhishingDetectingService
import io.novafoundation.nova.test_shared.any
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PhishingSitesRepositoryImplTest {

    @Mock
    lateinit var phishingDao: PhishingSitesDao

    @Mock
    lateinit var phishingSitesApi: PhishingSitesApi

    private val phishingDetectingService: PhishingDetectingService by lazy {
        CompoundPhishingDetectingService(
            listOf(
                BlackListPhishingDetectingService(phishingDao),
                DomainListPhishingDetectingService(listOf("top"))
            )
        )
    }

    private val phishingSiteRepository by lazy {
        PhishingSitesRepositoryImpl(phishingDao, phishingSitesApi, phishingDetectingService)
    }


    @Test
    fun isPhishing() {
        runBlocking {
            // exact match
            runTest(
                dbItems = listOf("host.com"),
                checkingUrl = "http://host.com",
                expectedResult = true
            )

            // subdomain
            runTest(
                dbItems = listOf("host.com"),
                checkingUrl = "http://sub.host.com",
                expectedResult = true
            )

            // sub-subdomain
            runTest(
                dbItems = listOf("host.com"),
                checkingUrl = "http://sub2.sub1.host.com",
                expectedResult = true
            )

            // ignore path and other url elements
            runTest(
                dbItems = listOf("host.com"),
                checkingUrl = "http://host.com/path?arg=1",
                expectedResult = true
            )

            // no prefix trigger
            runTest(
                dbItems = listOf("host.com"),
                checkingUrl = "http://prefixed-host.com",
                expectedResult = false
            )

            // ignore host in query
            runTest(
                dbItems = listOf("host.com"),
                checkingUrl = "http://valid.com?redirectUrl=host.com",
                expectedResult = false
            )

            // top url is always phishing
            runTest(
                dbItems = listOf(),
                checkingUrl = "http://invalid.host.top",
                expectedResult = true
            )
        }
    }

    private suspend fun runTest(
        dbItems: List<String>,
        checkingUrl: String,
        expectedResult: Boolean
    ) {
        reset(phishingDao)
        given(phishingDao.isPhishing(any())).willAnswer { mock ->
            val hostSuffixes = mock.getArgument<List<String>>(0).toSet()

            dbItems.any { it in hostSuffixes }
        }

        val isPhishing = phishingSiteRepository.isPhishing(checkingUrl)

        assertEquals(expectedResult, isPhishing)
    }
}
