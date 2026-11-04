package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl
import retrofit2.Response

class BittensorAlphaYieldSource(
    private val client: BittensorBackendClient,
    private val estimatesApi: BittensorEstimatesApi
) {

    private val requests = Semaphore(4)

    suspend fun alphaYields(base: HttpUrl, chain: Chain, subnet: BittensorNetuid.Subnet): BittensorAlphaYieldsRemote = client.get(
        url = bittensorBackendUrl(base, "subnets/${subnet.value}/yields/alpha"),
        type = BittensorAlphaYieldsRemote::class.java,
        allowStale = true,
        validate = { validateAlphaYields(it, chain, subnet) },
        fetch = ::fetchAlphaYields
    )

    fun observeAlphaYields(chain: Chain, subnet: BittensorNetuid.Subnet): Flow<BittensorAlphaYieldsRemote> = client.observe(
        url = { client.url("subnets/${subnet.value}/yields/alpha") },
        type = BittensorAlphaYieldsRemote::class.java,
        validate = { validateAlphaYields(it, chain, subnet) },
        fetch = ::fetchAlphaYields
    )

    private suspend fun fetchAlphaYields(url: String): Response<BittensorAlphaYieldsRemote> = requests.withPermit {
        estimatesApi.alphaYields(url)
    }

    private fun validateAlphaYields(body: BittensorAlphaYieldsRemote, chain: Chain, subnet: BittensorNetuid.Subnet) {
        requireAvailableBittensorComponent(body.meta?.components?.alphaYield, "Yield source")
        requireNotNull(body.items).forEach { mapBittensorValidatorYield(it, chain, subnet) }
    }
}
