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
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class BittensorWalletIsolationTest {

    private val accounts = mock(AccountRepository::class.java)

    private val extrinsics = mock(ExtrinsicService::class.java)
    private val wallets = mock(WalletRepository::class.java)

    private val submission = mock(BittensorSubmissionExecutor::class.java)

    private val validators = mock(BittensorSubnetValidatorRepository::class.java)

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

    private val selectedValidatorYield = BigDecimal("6.5")

    private val repository = object : BittensorStakingRepository {
        override suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator) = selectedValidatorYield

        override fun observeRootStakingDetails() = flowOf(details)

        override fun observeRootStakingDetails(metaId: Long) = flowOf(details)

        override fun observeRootAccountDetails(metaId: Long) = flowOf(details)
    }

    @Test
    fun `fee estimation does not depend on globally selected wallet`() = runBlocking {
        val request = BittensorRootStakeRequest(
            1L,
            details.chain,
            validator,
            BigInteger.ONE
        )
        runCatching {
            interactor()
                .estimateRootStakeFee(request)
        }
        verifyNoInteractions(accounts, validators)
    }

    @Test
    fun `validator lookup returns an eligible selection without modifying repository state`() = runBlocking {
        val selected = mock(MetaAccount::class.java)
        `when`(selected.id).thenReturn(1L)
        `when`(accounts.getSelectedMetaAccount()).thenReturn(selected)
        val chosenValidator = BittensorValidator(
            AccountIdKey(ByteArray(32) { 2 }),
            "Chosen validator"
        )
        val row = BittensorSubnetValidator(
            chosenValidator,
            "chosen-address",
            BigDecimal.ZERO,
            true
        )
        `when`(validators.rootValidators(details.chain)).thenReturn(listOf(row))
        val interactor = interactor()

        val selectedValidator = interactor.rootValidator(details.chain, row.address)

        assertEquals(chosenValidator, selectedValidator)
        assertEquals(
            validator,
            interactor.observeRootStakingDetails()
                .first().validator
        )
        verifyNoInteractions(extrinsics)
    }

    @Test
    fun `selected validator uses its own yield and existing stake`() = runBlocking {
        val selected = BittensorValidator(
            AccountIdKey(ByteArray(32) { 2 }),
            "Selected"
        )
        val defaultPosition = BittensorRootPosition(validator.hotkey, BigInteger.TEN)
        val selectedPosition = BittensorRootPosition(selected.hotkey, BigInteger.ONE)
        val initial = details.copy(
            annualPercentageYield = BigDecimal("3.0"),
            position = defaultPosition,
            positions = listOf(defaultPosition, selectedPosition)
        )
        val interactor = interactor()

        val displayed = interactor.detailsForValidator(initial, selected)

        assertEquals(selected, displayed.validator)
        assertEquals(selectedValidatorYield, displayed.annualPercentageYield)
        assertEquals(selectedPosition, displayed.position)
        assertEquals(defaultPosition, initial.position)
        assertEquals(initial.metaId, displayed.metaId)
    }

    private fun interactor() = BittensorStakingInteractor(
        repository,
        extrinsics,
        wallets,
        validators,
        submission
    )
}
