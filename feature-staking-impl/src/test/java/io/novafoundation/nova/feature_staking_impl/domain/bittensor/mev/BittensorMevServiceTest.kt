package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevExtrinsics
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevSealer
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevSource
import io.novafoundation.nova.runtime.extrinsic.Mortality
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import java.math.BigInteger

class BittensorMevServiceTest {

    private val calls = mutableListOf<String>()

    private val chain = mock(Chain::class.java)

    private val asset = mock(Chain.Asset::class.java)

    private val account = mock(MetaAccount::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val signers = mock(SignerProvider::class.java)

    private val signer = mock(NovaSigner::class.java) { invocation ->
        if (invocation.method.name == "preAuthorizeSignatures") calls += "authorize"
        if (invocation.method.name == "clearSigningAuthorization") calls += "clear"
        Answers.RETURNS_DEFAULTS.answer(invocation)
    }

    private val chainState = mock(ChainStateRepository::class.java)

    private val logger = mock(Logger::class.java)

    private val inner = mock(SendableExtrinsic::class.java)

    private val carrier = mock(SendableExtrinsic::class.java)

    private val accountId = ByteArray(32) { 1 }

    private val origin = SubmissionOrigin(accountId, accountId)

    private val fee = SubstrateFee(100.toBigInteger(), origin, asset)

    private val source = Source(calls)

    private val submittedNonces = mutableListOf<BigInteger>()

    private val submittedTimings = mutableListOf<BittensorMevTiming>()

    private val form: FormExtrinsicWithOrigin = {}

    private val extrinsics = mock(BittensorMevExtrinsics::class.java) { invocation ->
        when (invocation.method.name) {
            "build" -> {
                val nonce = invocation.getArgument<BigInteger>(3)
                val estimate = invocation.getArgument<Boolean>(4)
                if (!estimate) {
                    calls += "sign:$nonce"
                    submittedNonces += nonce
                    submittedTimings += invocation.getArgument<BittensorMevTiming>(2)
                }
                if (nonce == 11.toBigInteger()) inner else carrier
            }
            "fee" -> fee
            "carrier" -> form
            else -> Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private var sealedWith: ByteArray? = null

    private val sealer = mock(BittensorMevSealer::class.java) { invocation ->
        if (invocation.method.name == "seal") {
            sealedWith = invocation.getArgument<ByteArray>(1)
            calls += "seal"
            ByteArray(1200)
        } else {
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val submitted = BittensorMevExecution.Unconfirmed("0xinner")

    private val tracker = mock(BittensorMevTracker::class.java) { invocation ->
        if (invocation.method.name == "submitAndTrack") {
            calls += "broadcast"
            submitted
        } else {
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val service = BittensorMevService(accounts, signers, source, extrinsics, sealer, tracker, chainState, logger)

    @Test
    fun `authorization precedes fresh timing and fresh key follows signed inner`() = runTest {
        configure()
        assertSame(submitted, service.submit(chain, 1, form))
        assertEquals(listOf(11.toBigInteger(), 10.toBigInteger()), submittedNonces)
        assertSame(submittedTimings[0], submittedTimings[1])
        assertTrue(calls.indexOf("authorize") < calls.lastIndexOf("timing"))
        assertTrue(calls.indexOf("sign:11") < calls.lastIndexOf("key"))
        assertTrue(calls.lastIndexOf("key") < calls.indexOf("seal"))
        assertTrue(calls.indexOf("sign:10") < calls.indexOf("broadcast"))
        assertArrayEquals(ByteArray(1184) { 2 }, sealedWith)
        assertEquals("clear", calls.last())
    }

    @Test
    fun `missing readiness key never prompts or broadcasts`() = runTest {
        configure()
        source.keyAvailable = false
        assertTrue(runCatching { service.submit(chain, 1, form) }.exceptionOrNull() is BittensorMevNotReady)
        assertTrue("authorize" !in calls)
        verifyNoInteractions(tracker)
    }

    @Test
    fun `a key lost after signing never causes a clear broadcast`() = runTest {
        configure()
        source.keyLostAfterSigning = true
        assertTrue(runCatching { service.submit(chain, 1, form) }.exceptionOrNull() is BittensorMevNotReady)
        assertEquals(listOf(11.toBigInteger()), submittedNonces)
        verifyNoInteractions(tracker)
        assertEquals("clear", calls.last())
    }

    @Test
    fun `insufficient native balance fails readiness before authorization`() = runTest {
        configure()
        source.transferable = 209.toBigInteger()
        assertTrue(runCatching { service.submit(chain, 1, form) }.exceptionOrNull() is BittensorMevNotReady)
        assertTrue("authorize" !in calls)
        verifyNoInteractions(tracker)
    }

    @Test
    fun `external and watch only wallets never quote the shield`() = runTest {
        configure()
        for (type in LightMetaAccount.Type.entries.filter { it != LightMetaAccount.Type.SECRETS }) {
            `when`(account.type).thenReturn(type)
            val availability = service.quote(chain, 1, form) as BittensorMevAvailability.Unavailable
            assertEquals(BittensorMevUnavailableReason.UNSUPPORTED_WALLET, availability.reason)
        }
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `cancelled PIN never signs or broadcasts and clears the authorization`() = runTest {
        configure()
        `when`(signer.preAuthorizeSignatures(2)).thenAnswer { throw SigningCancelledException() }
        assertTrue(runCatching { service.submit(chain, 1, form) }.exceptionOrNull() is SigningCancelledException)
        assertTrue(submittedNonces.isEmpty())
        verifyNoInteractions(tracker)
        assertEquals("clear", calls.last())
    }

    private suspend fun configure() {
        `when`(chain.id).thenReturn("chain")
        `when`(account.type).thenReturn(LightMetaAccount.Type.SECRETS)
        `when`(account.accountIdIn(chain)).thenReturn(accountId)
        `when`(accounts.getMetaAccount(1)).thenReturn(account)
        `when`(signers.rootSignerFor(account)).thenReturn(signer)
        `when`(signer.getSigningHierarchy()).thenReturn(SubmissionHierarchy(account, CallExecutionType.IMMEDIATE))
        `when`(chainState.expectedBlockTimeInMillis("chain")).thenReturn(12_000.toBigInteger())
        `when`(inner.extrinsicHex).thenReturn("0x010203")
        `when`(carrier.extrinsicHex).thenReturn("0x040506")
    }

    private class Source(private val calls: MutableList<String>) : BittensorMevSource {

        var keyAvailable = true

        var keyLostAfterSigning = false

        var transferable = 210.toBigInteger()

        override suspend fun nextKey(chain: Chain): ByteArray? {
            calls += "key"
            if (!keyAvailable || keyLostAfterSigning && "sign:11" in calls) return null
            return ByteArray(1184) { if ("sign:11" in calls) 2 else 1 }
        }

        override suspend fun funds(chain: Chain, accountId: ByteArray) = BittensorMevFunds(transferable, 10.toBigInteger())

        override suspend fun timing(chain: Chain, address: String): BittensorMevTiming {
            calls += "timing"
            return BittensorMevTiming(10.toBigInteger(), 100, Mortality(Era.Mortal(8, 4), "0xbirth"))
        }
    }
}
