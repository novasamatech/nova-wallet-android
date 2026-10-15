package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class BittensorWalletIsolationTest {

    private val accounts = mock(AccountRepository::class.java)
    private val extrinsics = mock(ExtrinsicService::class.java)
    private val wallets = mock(WalletRepository::class.java)

    private val validators = mock(BittensorSubnetValidatorRepository::class.java)
    private val state = BittensorStakingFlowState()

    private val fee = mock(Fee::class.java)
    private val validator = BittensorValidator(AccountIdKey(ByteArray(32)), "Validator")
    private val details = BittensorRootStakingDetails(
        metaId = 1,
        chain = mock(Chain::class.java),
        asset = mock(Asset::class.java),
        validator = validator,
        annualPercentageYield = null,
        dataStatus = BittensorDataStatus.Fresh
    )
    private val repository = object : BittensorStakingRepository {
        override fun observeRootStakingDetails() = flowOf(details)
    }

    @Test
    fun `preparing after wallet switch does not estimate or submit an extrinsic`() = runBlocking {
        selectAnotherWallet()

        val failure = runCatching { interactor().prepareRootStake(BittensorRootStakeRequest(1L, details.chain, validator, BigInteger.ONE)) }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        verifyNoInteractions(extrinsics)
    }

    @Test
    fun `execution after wallet switch never signs with the new wallet`() = runBlocking {
        selectAnotherWallet()
        val draft = BittensorRootStakeDraft(1, BigInteger.ONE, validator, fee)
        state.setDraft(draft)

        val failure = runCatching { interactor().executeRootStake(details.chain, draft).getOrThrow() }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        verifyNoInteractions(extrinsics)
    }

    @Test
    fun `fee request after wallet switch does not estimate a fee`() = runBlocking {
        val selected = mock(MetaAccount::class.java)
        `when`(selected.id).thenReturn(2L)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(selected)
        val request = BittensorRootStakeRequest(1L, details.chain, validator, BigInteger.ONE)
        assertTrue(runCatching { interactor().estimateRootStakeFee(request) }.exceptionOrNull() is IllegalStateException)
        verifyNoInteractions(extrinsics, validators)
    }

    @Test
    fun `manual root choice overrides the default only after checking eligibility`() = runBlocking {
        val selected = mock(MetaAccount::class.java)
        `when`(selected.id).thenReturn(1L)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(selected)
        val chosenValidator = BittensorValidator(AccountIdKey(ByteArray(32) { 2 }), "Chosen validator")
        val row = BittensorSubnetValidator(chosenValidator, "chosen-address", BigDecimal.ZERO, true)
        `when`(validators.rootValidators(details.chain)).thenReturn(listOf(row))
        val interactor = interactor()

        interactor.selectValidator(details.copy(validator = null), row.address)

        assertEquals(chosenValidator, interactor.observeRootStakingDetails().first().validator)
        verifyNoInteractions(extrinsics)
    }

    private suspend fun selectAnotherWallet() {
        val selected = mock(MetaAccount::class.java)
        `when`(selected.id).thenReturn(2L)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(selected)
    }

    private fun interactor() = BittensorStakingInteractor(repository, extrinsics, state, accounts, wallets, validators)
}
