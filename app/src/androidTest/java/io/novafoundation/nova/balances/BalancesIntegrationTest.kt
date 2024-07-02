package io.novafoundation.nova.balances

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.domain.account.model.DefaultMetaAccount
import io.novafoundation.nova.runtime.BuildConfig.TEST_CHAINS_URL
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.extrinsic.systemRemark
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.wsrpc.networkStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.net.URL
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class BalancesIntegrationTest(
    private val testChainId: String,
    private val testChainName: String,
    private val testAccount: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data(): List<Array<String?>> {
            val arrayOfNetworks: Array<TestData> = Gson().fromJson(URL(TEST_CHAINS_URL).readText())
            return arrayOfNetworks.map { arrayOf(it.chainId, it.name, it.account) }
        }

        class TestData(
            val chainId: String,
            val name: String,
            val account: String?
        )
    }

    private val maxAmount = BigInteger.valueOf(10).pow(30)

    private val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    private val accountApi = FeatureUtils.getFeature<AccountFeatureComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        AccountFeatureApi::class.java
    )

    private val chainRegistry = runtimeApi.chainRegistry()
    private val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    private val remoteStorage = runtimeApi.remoteStorageSource()

    private val extrinsicService = accountApi.extrinsicService()

    @Before
    fun before() = runBlocking {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)
    }

    @Test
    fun testBalancesLoading() = runBlocking(Dispatchers.Default) {
        val chains = chainRegistry.getChain(testChainId)

        val freeBalance = testBalancesInChainAsync(chains, testAccount)?.data?.free ?: error("Balance was null")

        assertTrue("Free balance: $freeBalance is less than $maxAmount", maxAmount > freeBalance)
        assertTrue("Free balance: $freeBalance is greater than 0", ZERO < freeBalance)
    }

    @Test
    fun testFeeLoading() = runBlocking(Dispatchers.Default) {
        val chains = chainRegistry.getChain(testChainId)

        testFeeLoadingAsync(chains)

        Unit
    }

    private suspend fun testBalancesInChainAsync(chain: Chain, currentAccount: String): AccountInfo? {
        return coroutineScope {
            try {
                withTimeout(80.seconds) {
                    remoteStorage.query(
                        chainId = chain.id,
                        keyBuilder = { it.metadata.system().storage("Account").storageKey(it, currentAccount.fromHex()) },
                        binding = { scale, runtime -> scale?.let { bindAccountInfo(scale, runtime) } }
                    )
                }
            } catch (e: Exception) {
                throw Exception("Socket state: ${chainRegistry.getSocket(chain.id).networkStateFlow().first()}, error: ${e.message}", e)
            }
        }
    }

    private suspend fun testFeeLoadingAsync(chain: Chain) {
        return coroutineScope {
            withTimeout(80.seconds) {
                extrinsicService.estimateFee(chain, testTransactionOrigin()) {
                    systemRemark(byteArrayOf(0))

                    val haveBatch = runtime.metadata.hasModule("Utility")
                    if (haveBatch) {
                        systemRemark(byteArrayOf(0))
                    }
                }
            }
        }
    }

    private fun testTransactionOrigin(): TransactionOrigin = TransactionOrigin.Wallet(
        createTestMetaAccount()
    )

    private fun createTestMetaAccount(): MetaAccount {
        val metaAccount = DefaultMetaAccount(
            id = 0,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
            substratePublicKey = testAccount.fromHex(),
            substrateCryptoType = CryptoType.SR25519,
            substrateAccountId = testAccount.fromHex(),
            ethereumAddress = testAccount.fromHex(),
            ethereumPublicKey = testAccount.fromHex(),
            isSelected = true,
            name = "Test",
            type = LightMetaAccount.Type.WATCH_ONLY,
            status = LightMetaAccount.Status.ACTIVE,
            chainAccounts = emptyMap(),
            proxy = null
        )
        return metaAccount
    }
}
