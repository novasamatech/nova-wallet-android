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
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigInteger
import java.math.BigInteger.ZERO
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


@RunWith(Parameterized::class)
class BalancesIntegrationTest(
    private val testChainId: String,
    private val testChainName: String,
    private val testAccount: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Getting balance for {1} network")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "f1cf9022c7ebb34b162d5b5e34e705a5a740b2d0ecc1009fb89023e62a488108",
                    "Shiden",
                    "0x6a893a9f3cf8a97a2779539d4291324f948b0c361f37f2074dbc130a4950224c"
                ),
                arrayOf(
                    "91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3",
                    "Polkadot",
                    "0x7a28037947ecebe0dd86dc0e910911cb33185fd0714b37b75943f67dcf9b6e7c"
                ),
                arrayOf(
                    "b0a8d493285c2df73290dfb7e61f870f17b41801197a149ca93654499ea3dafe",
                    "Kusama",
                    "0x7a28037947ecebe0dd86dc0e910911cb33185fd0714b37b75943f67dcf9b6e7c"
                ),
                arrayOf(
                    "e143f23803ac50e8f6f8e62695d1ce9e4e1d68aa36c1cd2cfd15340213f3423e",
                    "Westend",
                    "0x7a28037947ecebe0dd86dc0e910911cb33185fd0714b37b75943f67dcf9b6e7c"
                ),
                arrayOf(
                    "48239ef607d7928874027a43a67689209727dfb3d3dc5e5b03a39bdc2eda771a",
                    "Statemine",
                    "0x14c8e8b6a677a898ec5eb3ff2af5af089227112fc963884b9fd54f24937f5b4d"
                ),
                arrayOf(
                    "baf5aabe40646d11f0ee8abbdc64f4a4b7674925cba08e4a05ff9ebed6e2126b",
                    "Karura",
                    "0xdad0a28c620ba73b51234b1b2ae35064d90ee847e2c37f9268294646c5af65eb"
                ),
                arrayOf(
                    "fe58ea77779b7abda7da4ec526d14db9b1e9cd40a217c34892af80a9b332b76d",
                    "Moonbeam",
                    "0x6d6f646c43726f77646c6f610000000000000000"
                ),
                arrayOf(
                    "401a1f9dca3da46f5c4091016c8a2f26dcea05865116b286f60f668207d1474b",
                    "Moonriver",
                    "0x6d6f646c43726f77646c6f610000000000000000"
                ),
                arrayOf(
                    "9f28c6a68e0fc9646eff64935684f6eeeece527e37bbe1f213d22caa1d9d6bed",
                    "Bifrost",
                    "0xee98c02253479726b456f3f7c5a9014d0198f53717de881c15f85becd706313c"
                ),
                arrayOf(
                    "4d812836a05a7ea37767325acadff956ce472474dd44dd2e7f15d16fbfc68cdb",
                    "Altair",
                    "0x6d6f646c70792f74727372790000000000000000000000000000000000000000"
                ),
                arrayOf(
                    "9d3ea49f13d993d093f209c42f97a6b723b84d3b9aa8461be9bb19ee13e7b4fd",
                    "Parallel Heiko",
                    "0x0c2017a4f115c013d899b494c955a7ec4cc9786a3997f1823baacc213896a35a"
                ),
                arrayOf(
                    "742a2ca70c2fda6cee4f8df98d64c4c670a052d9568058982dad9d5a7a135c5b",
                    "Edgeware",
                    "0x6d6f646c70792f74727372790000000000000000000000000000000000000000"
                ),
                arrayOf(
                    "d43540ba6d3eb4897c28a77d48cb5b729fea37603cbbfc7a86a73b72adb3be8d",
                    "Khala",
                    "0x6d6f646c7068616c612f62670000000000000000000000000000000000000000"
                ),
                arrayOf(
                    "411f057b9107718c9624d6aa4a3f23c1653898297f3d4d529d9bb6511a39dd21",
                    "KILT Spiritnet",
                    "0xf213f5635dd0a00c44c64983f5067b89448e670208fb2068832f52702ac33b51"
                ),
                arrayOf(
                    "4ac80c99289841dd946ef92765bf659a307d39189b3ce374a92b5f0415ee17a1",
                    "Calamari",
                    "0xb950066a74e6891ba1df54b869e684fec001e0fd1aa7425024f6e44acd439993"
                ),
                arrayOf(
                    "fc41b9bd8ef8fe53d58c7ea67c794c7ec9a73daf05e6d54b14ff6342c99ba64c",
                    "Acala",
                    "0x5336f96b54fa1832d517549bbffdfba2cae8983b8dcf65caff82d616014f5951"
                ),
                arrayOf(
                    "9eb76c5184c4ab8679d2d5d819fdf90b9c001403e9e17da2e14b6d8aec4029c6",
                    "Astar",
                    "0xe470631233212bfb8e10b6d56203d8f7580f5022a8f8207a1bb21c404dc68e6b"
                ),
                arrayOf(
                    "f22b7850cdd5a7657bbfd90ac86441275bbc57ace3d2698a740c7b0ec4de5ec3",
                    "Bit. Country",
                    "0x54fabff72b8ec769b862e4e841837cd394b59910c8507ec6b753e7b89364cf60"
                ),
                arrayOf(
                    "5c7bd13edf349b33eb175ffae85210299e324d852916336027391536e686f267",
                    "Clover",
                    "0xb6bede6cb32acce92409a782541fa2d8f3edaeeeab74ef28fb002cbec206db1e"
                ),
                arrayOf(
                    "8bf43860c54d5520fd0ec7afa15c2912a1d3b0e4cf132a5838a83b171dad4c70", //wrong genesis
                    "Basilisk",
                    "0x1ed10b8070552fe156b9c80930d3106723a987d8daf575c96c015def0fb5990f"
                ),
                arrayOf(
                    "cd4d732201ebe5d6b014edda071c4203e16867305332301dc8d092044b28e554",
                    "QUARTZ",
                    "0x70c17d7ec00783fcf263b1f7cfa493ad62177d45f9b5ef77a3848b3a2ed9f05e"
                ),
                arrayOf(
                    "e61a41c53f5dcd0beb09df93b34402aada44cb05117b71059cce40a2723a4e97",
                    "Parallel",
                    "0xd24bfa7e01cb86e6eab810a04a61d7bc3c32c094afd5b4739194c9583a69d238"
                ),
                arrayOf(
                    "631ccc82a078481584041656af292834e1ae6daab61d2875b4dd0c14bb9b17bc",
                    "Robonomics",
                    "0x6d6f646c70792f74727372790000000000000000000000000000000000000000"
                ),
                arrayOf(
                    "6811a339673c9daa897944dcdac99c6e2939cc88245ed21951a0a3c9a2be75bc",
                    "Picasso",
                    "0xe29a3f1571721bdbda403d5ebaf976b6ce9aab4617a29c13794c4ecc19f48d7d"
                ),
                arrayOf(
                    "da5831fbc8570e3c6336d0d72b8c08f8738beefec812df21ef2afc2982ede09c",
                    "Litmus",
                    "0x54191c48bef94a4b91384c52dc9c4e4e56c176c954ec841dc9d29cd2a4ffe76b"
                ),
                arrayOf(
                    "f195ef30c646663a24a3164b307521174a86f437c586397a43183c736a8383c1",
                    "Integritee Soloсhain",
                    "0x03bd97c3a34229dbdd4fc3b3a8c0647e9369c5c44555f252f2307d5efe7f4abd"
                ),
                arrayOf(
                    "b3db41421702df9a7fcac62b53ffeac85f7853cc4e689e0b93aeb3db18c09d82",
                    "Centrifuge Parachain",
                    "0xb03cd3fb823de75f888ac647105d7820476a6b1943a74af840996d2b28e64017"
                ),
                arrayOf(
                    "0bd72c1c305172e1275278aaeb3f161e02eccb7a819e63f62d47bd53a28189f8",
                    "Subsocial Solochain",
                    "0x24d6d8fc5d051fd471e275f14c83e95287d2b863e4cc802de1f78dea06c6ca78"
                ),
                arrayOf(
                    "1bf2a2ecb4a868de66ea8610f2ce7c8c43706561b6476031315f6640fe38e060",
                    "Zeitgeist",
                    "0x524e9aac979cbb9ecdb7acd1635755c3b15696321a3345ca77f0ab0ae23f675a"
                ),
                arrayOf(
                    "52149c30c1eb11460dce6c08b73df8d53bb93b4a15d0a2e7fd5dafe86a73c0da",
                    "KICO",
                    "0xfcacdc1b0849908f55623b4249ab6c63823122703c6fa9e223e15c0b0fffd371"
                ),
                arrayOf(
                    "a3d114c2b8d0627c1aa9b134eafcf7d05ca561fdc19fb388bb9457f81809fb23",
                    "Nodle Solochain",
                    "0xf233477e8d4e36baafb87987c74ae24e36cf33eb54485998e012e17acc421808"
                )
            )
        }
    }

    private val maxAmount = BigInteger.valueOf(10).pow(30)

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

        val freeBalance = errorMessage?.map { it?.data?.free }?.getOrNull() ?: throw errorMessage?.exceptionOrNull()!!
        assertTrue("Free balance: $freeBalance is less than $maxAmount", maxAmount > freeBalance)
        assertTrue("Free balance: $freeBalance is greater than 0", ZERO < freeBalance)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun CoroutineScope.testBalancesInChainAsync(chain: Chain): Result<AccountInfo?> {
        val currentAccount = testAccount.fromHex()
        return coroutineScope {
            runCatching {
                withTimeout(60.seconds) {
                    remoteStorage.query(
                        chainId = chain.id,
                        keyBuilder = { it.metadata.system().storage("Account").storageKey(it, currentAccount) },
                        binding = { scale, runtime -> scale?.let { bindAccountInfo(scale, runtime) } }
                    )
                }
            }
        }
    }
}
