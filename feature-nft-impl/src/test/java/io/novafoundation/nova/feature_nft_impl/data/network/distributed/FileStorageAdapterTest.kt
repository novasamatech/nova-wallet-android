package io.novafoundation.nova.feature_nft_impl.data.network.distributed

import org.junit.Assert.assertEquals
import org.junit.Test

class FileStorageAdapterTest {


    @Test
    fun `should adapt ipfs to http`() {
        runTest(
            initial = "ipfs://ipfs/bafkreig7jn6iwz4fo3mwkl3ndljbrf7ot4mwyvpmzrj4vm2nvwzw6dysb4",
            expected = "${FileStorage.IPFS.defaultHttpsGateway}/ipfs/bafkreig7jn6iwz4fo3mwkl3ndljbrf7ot4mwyvpmzrj4vm2nvwzw6dysb4"
        )

        runTest(
            initial = "ipfs://ipfs/bafkreig7jn6iwz4fo3mwkl3ndljbrf7ot4mwyvpmzrj4vm2nvwzw6dysb4",
            customGateways = mapOf(FileStorage.IPFS to "custom.domain.com"),
            expected = "custom.domain.com/ipfs/bafkreig7jn6iwz4fo3mwkl3ndljbrf7ot4mwyvpmzrj4vm2nvwzw6dysb4"
        )
    }

    @Test
    fun `should leave http and https as is`() {
        runTest(
            initial = "https://singular.rmrk.app/api/rmrk1/account/EkLXe943A4Ceu8rF4a2Wb8s5BHuTdTcEszJJCgsEPwAiGCi",
            expected = "https://singular.rmrk.app/api/rmrk1/account/EkLXe943A4Ceu8rF4a2Wb8s5BHuTdTcEszJJCgsEPwAiGCi"
        )

        runTest(
            initial = "http://singular.rmrk.app/api/rmrk1/account/EkLXe943A4Ceu8rF4a2Wb8s5BHuTdTcEszJJCgsEPwAiGCi",
            expected = "http://singular.rmrk.app/api/rmrk1/account/EkLXe943A4Ceu8rF4a2Wb8s5BHuTdTcEszJJCgsEPwAiGCi"
        )
    }

    private fun runTest(
        initial: String,
        expected: String,
        customGateways: Map<FileStorage, String> = emptyMap()
    ) {
        val actual = FileStorageAdapter.adaptToHttps(initial, customGateways)

        assertEquals(expected, actual)
    }
}
