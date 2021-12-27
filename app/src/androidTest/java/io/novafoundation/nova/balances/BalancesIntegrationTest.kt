package io.novafoundation.nova.balances

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


@RunWith(Parameterized::class)
class BalancesIntegrationTest(val testChainId: String, val testChainName: String) {

    companion object  {
        @JvmStatic
        @Parameterized.Parameters(name= "Getting balance for {1} network")
        fun data() : Collection<Array<Any>> {
            return listOf(
                arrayOf("f1cf9022c7ebb34b162d5b5e34e705a5a740b2d0ecc1009fb89023e62a488108", "Shiden"),
                arrayOf("91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3", "Polkadot"),
                arrayOf("b0a8d493285c2df73290dfb7e61f870f17b41801197a149ca93654499ea3dafe", "Kusama"),
                arrayOf("e143f23803ac50e8f6f8e62695d1ce9e4e1d68aa36c1cd2cfd15340213f3423e", "Westend"),
                arrayOf("48239ef607d7928874027a43a67689209727dfb3d3dc5e5b03a39bdc2eda771a", "Statemine"),
                arrayOf("baf5aabe40646d11f0ee8abbdc64f4a4b7674925cba08e4a05ff9ebed6e2126b", "Karura"),
                arrayOf("fe58ea77779b7abda7da4ec526d14db9b1e9cd40a217c34892af80a9b332b76d", "Moonbeam"),
                arrayOf("401a1f9dca3da46f5c4091016c8a2f26dcea05865116b286f60f668207d1474b", "Moonriver"),
                arrayOf("9f28c6a68e0fc9646eff64935684f6eeeece527e37bbe1f213d22caa1d9d6bed", "Bifrost"),
                arrayOf("4d812836a05a7ea37767325acadff956ce472474dd44dd2e7f15d16fbfc68cdb", "Altair"),
                arrayOf("9d3ea49f13d993d093f209c42f97a6b723b84d3b9aa8461be9bb19ee13e7b4fd", "Parallel Heiko"),
                arrayOf("742a2ca70c2fda6cee4f8df98d64c4c670a052d9568058982dad9d5a7a135c5b", "Edgeware"),
                arrayOf("d43540ba6d3eb4897c28a77d48cb5b729fea37603cbbfc7a86a73b72adb3be8d", "Khala"),
                arrayOf("411f057b9107718c9624d6aa4a3f23c1653898297f3d4d529d9bb6511a39dd21", "KILT Spiritnet"),
                arrayOf("4ac80c99289841dd946ef92765bf659a307d39189b3ce374a92b5f0415ee17a1", "Calamari"),
                arrayOf("fc41b9bd8ef8fe53d58c7ea67c794c7ec9a73daf05e6d54b14ff6342c99ba64c", "Acala"),
                arrayOf("9eb76c5184c4ab8679d2d5d819fdf90b9c001403e9e17da2e14b6d8aec4029c6", "Astar"),
                arrayOf("f22b7850cdd5a7657bbfd90ac86441275bbc57ace3d2698a740c7b0ec4de5ec3", "Bit. Country"),
                arrayOf("5c7bd13edf349b33eb175ffae85210299e324d852916336027391536e686f267", "Clover"),
                arrayOf("8bf43860c54d5520fd0ec7afa15c2912a1d3b0e4cf132a5838a83b171dad4c70", "Basilisk"), //wrong genesis
                arrayOf("cd4d732201ebe5d6b014edda071c4203e16867305332301dc8d092044b28e554", "QUARTZ"),
                arrayOf("e61a41c53f5dcd0beb09df93b34402aada44cb05117b71059cce40a2723a4e97", "Parallel")
            )
        }
    }

    private val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    private val chainRegistry = runtimeApi.chainRegistry()
    private val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    private val remoteStorage = runtimeApi.remoteStorageSource()

    @Test
    fun testBalancesLoading() = runBlocking(Dispatchers.Default) {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)

        val chains = chainRegistry.currentChains.first()

        val errorMessage = chains.find { it.id == testChainId }?.let { it -> testBalancesInChainAsync(it) }

        assertEquals(kotlin.Result.success(null), errorMessage)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun CoroutineScope.testBalancesInChainAsync(chain: Chain): Result<AccountInfo?> {
        return coroutineScope {
            runCatching {
                withTimeout(60.seconds) {
                    remoteStorage.query(
                        chainId = chain.id,
                        keyBuilder = { it.metadata.system().storage("Account").storageKey(it, chain.sampleAccountId()) },
                        binding = { scale, runtime -> scale?.let { bindAccountInfo(scale, runtime) } }
                    )
                }
            }
        }
    }

    private fun Chain.sampleAccountId() = if (isEthereumBased) {
        ByteArray(20) { 1 }
    } else {
        ByteArray(32) { 1 }
    }
}
