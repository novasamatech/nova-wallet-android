package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorCoverageInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorValidatorInfoViewModelTest {

    private val resources = mock(ResourceManager::class.java) { invocation ->
        if (invocation.method.name == "getString") invocation.arguments.joinToString("|") else Mockito.RETURNS_DEFAULTS.answer(invocation)
    }
    private val validatorInteractor = mock(BittensorValidatorInteractor::class.java)
    private val models = mutableListOf<BittensorValidatorInfoViewModel>()

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(validatorInteractor.chain()).thenThrow(IllegalStateException("Chain unavailable"))
        Unit
    }

    @After
    fun teardown() = runBlocking {
        models.forEach { it.coroutineContext.job.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `row snapshot is shown as status take and reward`() {
        val active = createViewModel(payload(inactive = false, annualYield = BigDecimal("5.11")))
        assertEquals(R.string.common_active.toString(), active.status.text)
        assertEquals(TableCellView.FieldStyle.POSITIVE, active.status.style)
        assertEquals("${R.string.bittensor_validator_info_take_value}|18", active.take)
        assertEquals("${R.string.bittensor_validator_info_reward_average}", active.reward.detail)

        val inactive = createViewModel(payload(inactive = true, annualYield = null))
        assertEquals(R.string.staking_nominator_status_inactive.toString(), inactive.status.text)
        assertEquals(TableCellView.FieldStyle.SECONDARY, inactive.reward.style)
        assertNull(inactive.reward.detail)
    }

    @Test
    fun `reward reads as APY and the info sheets explain root and subnet stake differently`() {
        val subnet = createViewModel(payload(inactive = false, annualYield = BigDecimal("5.94")))
        assertEquals("${R.string.staking_apy}|5.94%", subnet.reward.value)
        assertEquals("${R.string.bittensor_validator_info_total_staked_subnet}", subnet.totalStakeDescription)
        assertEquals("${R.string.bittensor_validator_info_take_description}", subnet.takeDescription)

        val root = createViewModel(payload(inactive = false, annualYield = null, subnetId = 0))
        assertEquals("${R.string.bittensor_validator_info_total_staked_root}", root.totalStakeDescription)
    }

    @Test
    fun `failed coverage reads as unknown instead of an error`() = runBlocking<Unit> {
        val viewModel = createViewModel(payload(inactive = false, annualYield = null))

        val state = withTimeout(5_000) { viewModel.coverage.first { it !is ExtendedLoadingState.Loading } }

        assertEquals(ExtendedLoadingState.Loaded("—"), state)
    }

    @Test
    fun `missing reported stake shows no amount`() = runBlocking<Unit> {
        val viewModel = createViewModel(payload(inactive = false, annualYield = null))

        assertNull(withTimeout(5_000) { viewModel.totalStake.first() })
    }

    @Test
    fun `stake that cannot be formatted resolves to no amount instead of loading forever`() = runBlocking<Unit> {
        val viewModel = createViewModel(payload(inactive = false, annualYield = null, stake = BigDecimal.TEN))

        assertNull(withTimeout(5_000) { viewModel.totalStake.first() })
    }

    private fun payload(inactive: Boolean, annualYield: BigDecimal?, stake: BigDecimal? = null, subnetId: Int = 5) = BittensorValidatorInfoPayload(
        subnetId = subnetId,
        address = "address",
        name = "Validator",
        stakeInNative = stake,
        commissionRate = BigDecimal("0.18"),
        inactive = inactive,
        annualYield = annualYield
    )

    private fun createViewModel(payload: BittensorValidatorInfoPayload) = BittensorValidatorInfoViewModel(
        payload = payload,
        router = mock(StakingRouter::class.java),
        validatorInteractor = validatorInteractor,
        coverageInteractor = mock(BittensorValidatorCoverageInteractor::class.java),
        subnetInteractor = mock(BittensorSubnetInteractor::class.java),
        addressIconGenerator = mock(AddressIconGenerator::class.java),
        amountFormatter = mock(BittensorAmountFormatter::class.java),
        tokenFormatter = mock(TokenFormatter::class.java),
        externalActions = mock(ExternalActions.Presentation::class.java),
        resources = resources,
        logger = mock(Logger::class.java)
    ).also { models += it }
}
