package io.novafoundation.nova.feature_dapp_impl.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlsTest {

    @Test
    fun `should ensure url protocol`() {
        runEnsureProtocolTest("onlyHost.com", "https://onlyHost.com")
        runEnsureProtocolTest("https://alreadyHttps.com", "https://alreadyHttps.com")
        runEnsureProtocolTest("http://withHttp.com", "https://withHttp.com")
    }

    private fun runEnsureProtocolTest(
        input: String,
        expected: String
    ) {
        val actual = Urls.ensureHttpsProtocol(input)

        assertEquals(expected, actual)
    }
}
