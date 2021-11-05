package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.default
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.transfer
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.encrypt.keypair.ethereum.EthereumKeypairFactory
import jp.co.soramitsu.fearless_utils.encrypt.seed.ethereum.EthereumSeedFactory
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

private val TEST_MNEMONIC = "second wire page void maid example hazard record assist funny flower enough"


@RunWith(AndroidJUnit4::class)
class MoonbaseSendIntagrationTest {

    val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    val chainRegistry = runtimeApi.chainRegistry()
    val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    val rpcCalls = runtimeApi.rpcCalls()

    val extrinsicBuilderFactory = runtimeApi.provideExtrinsicBuilderFactory()

    private val keypair = createKeypair(TEST_MNEMONIC)

    private fun createKeypair(mnemonic: String): Keypair {
        val seed = EthereumSeedFactory.deriveSeed(mnemonic, password = null).seed
        val junctions = BIP32JunctionDecoder.default().junctions

        return EthereumKeypairFactory.generate(seed, junctions)
    }

    @Test
    fun testTransfer() = runBlocking {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)
        val chain = chainRegistry.getChain("91bc6e169807aaa54802737e1c504b2577d4fafedd5a02c10293b1cd60e39527")

        val accountId = chain.accountIdOf("0x5eC0aa4d0dFF013E30978f954ca81779e8966d3A")

        val extrinsic = extrinsicBuilderFactory.create(chain, keypair, CryptoType.ECDSA)
            .transfer(accountId, chain.utilityAsset.planksFromAmount(BigDecimal.ONE), keepAlive = true)
            .build()

        val hash = rpcCalls.submitExtrinsic(chain.id, extrinsic)

        print(hash)
    }
}
