package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorCoverageInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
import org.junit.Assert.assertTrue
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
    private val logger = mock(Logger::class.java)
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
    fun `row snapshot is shown as status and take`() = runBlocking<Unit> {
        val active = createViewModel(payload(inactive = false))
        val activeStatus = withTimeout(5_000) { active.status.first() }
        assertEquals(R.string.common_active.toString(), activeStatus.text)
        assertEquals(TableCellView.FieldStyle.POSITIVE, activeStatus.style)
        assertEquals("${R.string.bittensor_validator_info_take_value}|18", withTimeout(5_000) { active.take.first() })

        val inactive = withTimeout(5_000) { createViewModel(payload(inactive = true)).status.first() }
        assertEquals(R.string.staking_nominator_status_inactive.toString(), inactive.text)
        assertEquals(TableCellView.FieldStyle.SECONDARY, inactive.style)
    }

    @Test
    fun `without a row snapshot the validator is read from the chain`() = runBlocking<Unit> {
        val chain = mock(Chain::class.java)
        val hotkey = AccountIdKey(ByteArray(32) { 7 })
        Mockito.doReturn(chain).`when`(validatorInteractor).chain()
        val address = chain.addressOf(hotkey)
        val validator = BittensorSubnetValidator(
            BittensorValidator(hotkey, "Validator"),
            address,
            commissionRate = BigDecimal("0.18"),
            selectable = false,
            inactive = true
        )
        `when`(validatorInteractor.chainValidator(BittensorNetuid.Subnet(5), hotkey)).thenReturn(validator)
        val viewModel = createViewModel(BittensorValidatorInfoPayload(subnetId = 5, address = address))

        val status = withTimeout(5_000) { viewModel.status.first() }

        assertEquals(R.string.staking_nominator_status_inactive.toString(), status.text)
        assertEquals("${R.string.bittensor_validator_info_take_value}|18", withTimeout(5_000) { viewModel.take.first() })
    }

    @Test
    fun `failed validator read offers a retry`() = runBlocking<Unit> {
        val viewModel = createViewModel(BittensorValidatorInfoPayload(subnetId = 5, address = "address"))

        val failure = withTimeout(5_000) { viewModel.retryEvents.first() }

        assertTrue(failure.retryable)
    }

    @Test
    fun `reward shows the reliable rate the screen loads itself`() = runBlocking<Unit> {
        val chain = mock(Chain::class.java)
        val hotkey = AccountIdKey(ByteArray(32) { 7 })
        Mockito.doReturn(chain).`when`(validatorInteractor).chain()
        `when`(validatorInteractor.yields(BittensorNetuid.Subnet(5), includeUnreliable = false)).thenReturn(mapOf(hotkey to BigDecimal("5.11")))
        val viewModel = createViewModel(payload(inactive = false, address = chain.addressOf(hotkey)))

        val reward = withTimeout(5_000) { viewModel.reward.first() }

        assertEquals("${R.string.bittensor_validator_info_reward_average}", reward.detail)
        assertEquals(TableCellView.FieldStyle.POSITIVE, reward.style)
    }

    @Test
    fun `failed reward read shows no rate`() = runBlocking<Unit> {
        val reward = withTimeout(5_000) { createViewModel(payload(inactive = false)).reward.first() }

        assertEquals("—", reward.value)
        assertNull(reward.detail)
        assertEquals(TableCellView.FieldStyle.SECONDARY, reward.style)
    }

    @Test
    fun `failed coverage reads as unknown instead of an error`() = runBlocking<Unit> {
        val viewModel = createViewModel(payload(inactive = false))

        val state = withTimeout(5_000) { viewModel.coverage.first { it !is ExtendedLoadingState.Loading } }

        assertEquals(ExtendedLoadingState.Loaded("—"), state)
    }

    @Test
    fun `missing reported stake shows no amount`() = runBlocking<Unit> {
        val viewModel = createViewModel(payload(inactive = false))

        assertNull(withTimeout(5_000) { viewModel.totalStake.first() })
    }

    @Test
    fun `subnet stake without a fresh price shows no amount`() = runBlocking<Unit> {
        `when`(validatorInteractor.stakeInNative(BittensorNetuid.Subnet(5), BigDecimal.TEN)).thenReturn(null)
        val viewModel = createViewModel(payload(inactive = false, stake = BigDecimal.TEN))

        assertNull(withTimeout(5_000) { viewModel.totalStake.first() })
    }

    @Test
    fun `stake that cannot be formatted resolves to no amount instead of loading forever`() = runBlocking<Unit> {
        `when`(validatorInteractor.stakeInNative(BittensorNetuid.Subnet(5), BigDecimal.TEN)).thenReturn(BigDecimal.TEN)
        val viewModel = createViewModel(payload(inactive = false, stake = BigDecimal.TEN))

        assertNull(withTimeout(5_000) { viewModel.totalStake.first() })
    }

    private fun payload(inactive: Boolean, stake: BigDecimal? = null, address: String = "address") = BittensorValidatorInfoPayload(
        subnetId = 5,
        address = address,
        detail = BittensorValidatorInfoPayload.Detail(
            name = "Validator",
            reportedStake = stake,
            commissionRate = BigDecimal("0.18"),
            inactive = inactive
        )
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
        retryController = BittensorRetryController(resources, logger),
        resources = resources,
        logger = logger
    ).also { models += it }
}
