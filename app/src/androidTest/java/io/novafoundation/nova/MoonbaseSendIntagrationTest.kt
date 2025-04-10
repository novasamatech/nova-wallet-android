package io.novafoundation.nova

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.default
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.TransferMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeComponent
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.ethereum.EthereumKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.seed.ethereum.EthereumSeedFactory
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
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

    private inner class TestSigner(
        private val testPayload: (SignerPayloadExtrinsic) -> Unit
    ) : NovaSigner {

        val accountId = ByteArray(32) { 1 }

        override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
            testPayload(payloadExtrinsic)
            return SignedExtrinsic(payloadExtrinsic, SignatureWrapper.Sr25519(ByteArray(64)))
        }

        override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
            error("Not implemented")
        }

        override suspend fun actualSignerAccountId(chain: Chain): AccountId {
            return accountId
        }

        override suspend fun modifyPayload(payloadExtrinsic: SignerPayloadExtrinsic): SignerPayloadExtrinsic {
            return payloadExtrinsic
        }
    }

    @Test
    fun testTransfer() = runBlocking {
        externalRequirementFlow.emit(ChainConnection.ExternalRequirement.ALLOWED)
        val chain = chainRegistry.getChain("91bc6e169807aaa54802737e1c504b2577d4fafedd5a02c10293b1cd60e39527")

        val accountId = chain.accountIdOf("0x0c7485f4AA235347BDE0168A59f6c73C7A42ff2C")
        val signer = TestSigner {_ ->}

        val extrinsic = extrinsicBuilderFactory.create(chain, signer, accountId)
            .nativeTransfer(accountId, chain.utilityAsset.planksFromAmount(BigDecimal.ONE), TransferMode.KEEP_ALIVE)
            .buildExtrinsic()

        val hash = rpcCalls.submitExtrinsic(chain.id, extrinsic)

        print(hash)
    }
}
