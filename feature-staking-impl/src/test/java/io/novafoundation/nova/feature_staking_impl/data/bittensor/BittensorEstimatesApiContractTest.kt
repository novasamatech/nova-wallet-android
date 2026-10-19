package io.novafoundation.nova.feature_staking_impl.data.bittensor

import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.Query

class BittensorEstimatesApiContractTest {

    @Test
    fun `operation history sends only the account and page query parameters`() {
        assertEquals(setOf("accountSubject", "page"), queryParameters("operations"))
    }

    @Test
    fun `yield pagination still supports a client selected page size`() {
        assertEquals(setOf("page", "pageSize"), queryParameters("alphaYields"))
    }

    private fun queryParameters(methodName: String): Set<String> {
        val method = BittensorEstimatesApi::class.java.declaredMethods.single { it.name == methodName }
        return method.parameterAnnotations
            .flatMap { it.filterIsInstance<Query>() }
            .map { it.value }
            .toSet()
    }
}
