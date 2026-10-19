package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class BittensorAddRootStakeTest {

    private val repository = mock(BittensorStakingRepository::class.java)

    private val extrinsics = mock(ExtrinsicService::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val validators = mock(BittensorSubnetValidatorRepository::class.java)

    private val submission = mock(BittensorSubmissionExecutor::class.java)

    private val interactor = BittensorStakingInteractor(repository, extrinsics, wallets, validators, submission)

    private val chain = mock(Chain::class.java)

    private val asset = mock(Asset::class.java)

    private val fixedValidator = BittensorValidator(AccountIdKey(ByteArray(32) { 2 }), "Existing position")

    private val defaultValidator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), "Default recommendation")

    @Test
    fun `add stake observes the explicit wallet and keeps the position validator`() = runBlocking<Unit> {
        `when`(chain.addressPrefix).thenReturn(42)
        val address = chain.addressOf(fixedValidator.hotkey.value)
        val position = BittensorRootPosition(fixedValidator.hotkey, BigInteger.TEN)
        val details = BittensorRootStakingDetails(7, chain, asset, defaultValidator, null, BittensorDataStatus.Fresh, positions = listOf(position))
        val candidate = BittensorSubnetValidator(fixedValidator, address, BigDecimal.ZERO, selectable = true)
        `when`(repository.observeRootAccountDetails(7)).thenReturn(flowOf(details))
        `when`(validators.rootValidators(chain)).thenReturn(listOf(candidate))

        val account = interactor.observeRootAccountDetails(7).first()
        val resolved = interactor.rootValidatorDetails(chain, address)
        val selected = interactor.detailsForFixedValidator(account, address, resolved)

        assertEquals(7L, selected.metaId)
        assertEquals(fixedValidator, selected.validator)
        assertEquals(position, selected.position)
        verify(repository, never()).observeRootStakingDetails()
    }

    @Test
    fun `unresolved fixed validator preserves balances and position without a default fallback`() {
        `when`(chain.addressPrefix).thenReturn(42)
        val address = chain.addressOf(fixedValidator.hotkey.value)
        val firstPosition = BittensorRootPosition(fixedValidator.hotkey, BigInteger.TEN)
        val initial = BittensorRootStakingDetails(
            7,
            chain,
            asset,
            defaultValidator,
            BigDecimal.TEN,
            BittensorDataStatus.Fresh,
            positions = listOf(firstPosition)
        )

        val selected = interactor.detailsForFixedValidator(initial, address, null)

        assertEquals(7L, selected.metaId)
        assertEquals(asset, selected.asset)
        assertEquals(firstPosition, selected.position)
        assertEquals(initial.positions, selected.positions)
        assertEquals(BittensorDataStatus.Fresh, selected.dataStatus)
        assertNull(selected.validator)
        assertNull(selected.annualPercentageYield)
    }

    @Test
    fun `an unavailable fixed validator cannot silently fall back to a recommendation`() = runBlocking {
        `when`(chain.addressPrefix).thenReturn(42)
        val address = chain.addressOf(fixedValidator.hotkey.value)
        `when`(validators.rootValidators(chain)).thenReturn(emptyList())

        val failure = runCatching { interactor.rootValidatorDetails(chain, address) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }
}
