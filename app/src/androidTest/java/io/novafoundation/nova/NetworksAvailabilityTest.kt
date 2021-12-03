package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.default
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.encrypt.keypair.ethereum.EthereumKeypairFactory
import jp.co.soramitsu.fearless_utils.encrypt.keypair.substrate.SubstrateKeypairFactory
import jp.co.soramitsu.fearless_utils.encrypt.seed.ethereum.EthereumSeedFactory
import jp.co.soramitsu.fearless_utils.hash.isPositive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

private const val TEST_MNEMONIC = "second wire page void maid example hazard record assist funny flower enough"

@RunWith(AndroidJUnit4::class)
class NetworksAvailabilityTest {

    val runtimeApi = FeatureUtils.getFeature<RuntimeComponent>(
        ApplicationProvider.getApplicationContext<Context>(),
        RuntimeApi::class.java
    )

    val chainRegistry = runtimeApi.chainRegistry()

    val externalRequirementFlow = runtimeApi.externalRequirementFlow()

    val rpcCalls = runtimeApi.rpcCalls()

    val extrinsicBuilderFactory = runtimeApi.provideExtrinsicBuilderFactory()

    val subCryptoType = CryptoType.SR25519

    fun createEthereumKeypair(mnemonic: String): Keypair {
        val seed = EthereumSeedFactory.deriveSeed(mnemonic, password = null).seed
        val junctions = BIP32JunctionDecoder.default().junctions

        return EthereumKeypairFactory.generate(seed, junctions)
    }

    fun createSubstrateKeypair(mnemonic: String, cryptoType: CryptoType): Keypair {
        return SubstrateKeypairFactory.generate(
            mapCryptoTypeToEncryption(cryptoType),
            ByteArray(32) { 1 },
            junctions = emptyList()
        )
    }


    @Test
    fun testBalance() = runBlocking {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)
        for ((_, chain) in chainRegistry.chainsById.first()) {

            var keypair = if (chain.isEthereumBased) {
                createEthereumKeypair(TEST_MNEMONIC)
            } else {
                createSubstrateKeypair(TEST_MNEMONIC, subCryptoType)
            }

            val accountId = keypair.publicKey

            val extrinsic = extrinsicBuilderFactory.create(chain, keypair, subCryptoType)
                .transfer(accountId, chain.utilityAsset.planksFromAmount(BigDecimal.ONE), keepAlive = true)
                .build()
            println("========================================="+chain.name)
            val fee = rpcCalls.getExtrinsicFee(chain.id, extrinsic)

            assert(fee.isPositive())
        }

    }

}
