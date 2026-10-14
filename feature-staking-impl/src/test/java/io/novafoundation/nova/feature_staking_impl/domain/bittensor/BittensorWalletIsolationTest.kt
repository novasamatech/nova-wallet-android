package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigInteger

class BittensorWalletIsolationTest {

    private val accounts = mock(AccountRepository::class.java)
    private val extrinsics = mock(ExtrinsicService::class.java)
    private val wallets = mock(WalletRepository::class.java)
    private val state = BittensorStakingFlowState()
    private val validator = BittensorValidator(AccountIdKey(ByteArray(32)), "Validator", null)
    private val details = BittensorRootStakingDetails(
        metaId = 1,
        chain = mock(Chain::class.java),
        asset = mock(Asset::class.java),
        validator = validator,
        annualPercentageYield = null,
        position = null,
        dataStatus = BittensorDataStatus.Fresh
    )
    private val repository = object : BittensorStakingRepository {
        override fun observeRootStakingDetails() = flowOf(details)
        override fun refreshPosition() = Unit
    }

    @Test
    fun `preparing after wallet switch does not estimate or submit an extrinsic`() = runBlocking {
        selectAnotherWallet()
        state.setDetails(details)

        val failure = runCatching { interactor().prepareRootStake(BigInteger.ONE) }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        verifyNoInteractions(extrinsics)
    }

    @Test
    fun `execution after wallet switch never signs with the new wallet`() = runBlocking {
        selectAnotherWallet()
        state.setDetails(details)
        state.setDraft(BittensorRootStakeDraft(1, BigInteger.ONE, validator, mock(Fee::class.java)))

        val failure = runCatching { interactor().executeRootStake() }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        verifyNoInteractions(extrinsics)
    }

    private suspend fun selectAnotherWallet() {
        val selected = mock(MetaAccount::class.java)
        `when`(selected.id).thenReturn(2L)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(selected)
    }

    private fun interactor() = BittensorStakingInteractor(repository, extrinsics, state, accounts, wallets)
}
