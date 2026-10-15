package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.notValidOrNull
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorStakeValidationsModule
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class BittensorStakePreflightValidationTest {

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val account = mock(MetaAccount::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val asset = mock(Asset::class.java)

    private val token = mock(Token::class.java)

    private val feeOrigin = SubmissionOrigin.singleOrigin(ByteArray(32))

    private val fee = SubstrateFee(BigInteger.TEN, feeOrigin, chainAsset)

    private val coldKey: AccountId = ByteArray(32) { 1 }

    private val hotkey: AccountId = ByteArray(32) { 2 }

    private var loadedColdKey: AccountId? = null

    private var snapshot = BittensorStakePreflight(true, true, true, false, false, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, null)

    private var sourceFailure: Exception? = null

    private var loads = 0

    private val repository = object : BittensorStakePreflightRepository {
        override suspend fun load(chainId: String, netuid: BittensorNetuid, coldKey: AccountId, hotkey: AccountId): BittensorStakePreflight {
            sourceFailure?.let { throw it }
            loads += 1
            loadedColdKey = coldKey
            return snapshot
        }
    }

    private val module = BittensorStakeValidationsModule()

    private val stakeValidationSystem = module.provideStakeValidationSystem()

    private val validation = BittensorStakePreflightValidation(repository, accounts, wallets, stakeValidationSystem)

    private val system = ValidationSystem(validation)

    private val payload = BittensorStakePreflightPayload(chain, 7L, BittensorNetuid.Root, hotkey, BigInteger.valueOf(100_000_000), fee)

    @Before
    fun setup() = runBlocking<Unit> {
        `when`(chain.id).thenReturn("bittensor")
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(account.accountIdIn(chain)).thenReturn(coldKey)
        `when`(accounts.getMetaAccount(7L)).thenReturn(account)
        `when`(wallets.assetFlow(7L, chainAsset)).thenReturn(flowOf(asset))
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.chainId).thenReturn("bittensor")
        `when`(asset.transferable).thenReturn(BigDecimal.ONE)
        `when`(asset.transferableInPlanks).thenReturn(BigInteger.TEN.pow(9))
    }

    @Test
    fun `validation uses the operation wallet without reading selected wallet`() = runBlocking<Unit> {
        assertNull(system.validate(payload).getOrThrow().notValidOrNull())
        assertArrayEquals(coldKey, loadedColdKey)
        verify(accounts, never()).getSelectedMetaAccount()
        verify(wallets).assetFlow(7L, chainAsset)
    }

    @Test
    fun `revalidation reads current restrictions and balance even after warning acceptance`() = runBlocking {
        assertNull(system.validate(payload).getOrThrow().notValidOrNull())
        snapshot = snapshot.copy(safeModeActive = true)
        val failure = system.validate(payload.copy(priceImpactAccepted = true)).getOrThrow().notValidOrNull()
        assertEquals(BittensorStakeValidationFailure.SAFE_MODE_ACTIVE, failure?.reason)
        assertEquals(2, loads)
    }

    @Test
    fun `failed storage read never becomes permissive defaults`() = runBlocking {
        val failure = IOException("RPC unavailable")
        sourceFailure = failure
        val result = system.validate(payload)
        assertEquals(IOException::class.java, result.exceptionOrNull()?.javaClass)
        assertEquals(failure.message, result.exceptionOrNull()?.message)
    }
}
