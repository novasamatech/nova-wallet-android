package io.novafoundation.nova.feature_wallet_impl.domain.asset

import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.IncomingDirection
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class RealAssetGetOptionsUseCaseTest {

    private val crossChainTransfers = mock(CrossChainTransfersUseCase::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val selectedAccount = mock(MetaAccount::class.java)

    private val useCase = RealAssetGetOptionsUseCase(crossChainTransfers, accounts)

    private val changeNow = GetAssetOption.DApp(
        name = "ChangeNOW",
        url = "https://changenow.io/embeds/exchange-widget/v2/widget.html?from=assethub&to=tao&link_id=59409968ba3831",
        supportedTokens = listOf("DOT", "USDT", "USDC").map { it.asTokenSymbol() }
    )

    @Before
    fun setUp() {
        `when`(selectedAccount.type).thenReturn(LightMetaAccount.Type.SECRETS)
        `when`(accounts.selectedMetaAccountFlow()).thenReturn(flowOf(selectedAccount))
    }

    @Test
    fun `native TAO offers ChangeNOW with partner URL and supported tokens without other funding providers`() = runBlocking {
        val tao = asset()

        assertEquals(setOf(GetAssetOption.Receive, changeNow), options(tao))
    }

    @Test
    fun `TAO symbol on another chain does not offer ChangeNOW`() = runBlocking {
        val unrelatedTao = asset(chainId = ChainGeneses.POLKADOT, symbol = "TAO")

        assertEquals(setOf(GetAssetOption.Receive), options(unrelatedTao))
    }

    @Test
    fun `non native asset on Bittensor does not offer ChangeNOW`() = runBlocking {
        val token = asset(type = Chain.Asset.Type.EvmErc20("0x0000000000000000000000000000000000000001"))

        assertEquals(setOf(GetAssetOption.Receive), options(token))
    }

    @Test
    fun `missing asset has no funding options`() = runBlocking {
        assertEquals(emptySet<GetAssetOption>(), options(null))
    }

    @Test
    fun `ordinary asset keeps receive cross chain and provider buy options`() = runBlocking {
        val ordinary = asset(chainId = ChainGeneses.POLKADOT, symbol = "DOT", buyAvailable = true)
        val incomingDirection = mock(IncomingDirection::class.java)

        assertEquals(
            setOf(GetAssetOption.Receive, GetAssetOption.CrossChain, GetAssetOption.Buy),
            options(ordinary, listOf(incomingDirection))
        )
    }

    @Test
    fun `watch only account keeps existing receive restriction for ordinary assets`() = runBlocking {
        `when`(selectedAccount.type).thenReturn(LightMetaAccount.Type.WATCH_ONLY)
        val ordinary = asset(chainId = ChainGeneses.POLKADOT, symbol = "DOT", buyAvailable = true)
        val incomingDirection = mock(IncomingDirection::class.java)

        assertEquals(setOf(GetAssetOption.CrossChain, GetAssetOption.Buy), options(ordinary, listOf(incomingDirection)))
    }

    @Test
    fun `switching away from native TAO removes ChangeNOW from an active observation`() = runBlocking {
        val selectedAsset = MutableStateFlow<Chain.Asset?>(asset())
        val incomingDirections = flowOf(emptyList<IncomingDirection>())
        `when`(crossChainTransfers.incomingCrossChainDirections(selectedAsset)).thenReturn(incomingDirections)
        val updates = Channel<Set<GetAssetOption>>(Channel.UNLIMITED)
        val observation = launch {
            useCase.observeAssetGetOptionsForSelectedAccount(selectedAsset).collect { updates.send(it) }
        }
        try {
            val initial = withTimeout(5_000) { updates.receive() }
            assertTrue(changeNow in initial)

            selectedAsset.value = asset(chainId = ChainGeneses.POLKADOT, symbol = "DOT")
            val changed = withTimeout(5_000) {
                var options = updates.receive()
                while (options.any { it is GetAssetOption.DApp }) {
                    options = updates.receive()
                }
                options
            }

            assertEquals(setOf(GetAssetOption.Receive), changed)
        } finally {
            observation.cancelAndJoin()
            updates.close()
        }
    }

    private suspend fun options(asset: Chain.Asset?, directions: List<IncomingDirection> = emptyList()): Set<GetAssetOption> {
        val assetFlow = flowOf(asset)
        val incomingDirections = flowOf(directions)
        `when`(crossChainTransfers.incomingCrossChainDirections(assetFlow)).thenReturn(incomingDirections)
        return useCase.observeAssetGetOptionsForSelectedAccount(assetFlow).first()
    }

    private fun asset(
        chainId: String = ChainGeneses.BITTENSOR,
        symbol: String = "TAO",
        type: Chain.Asset.Type = Chain.Asset.Type.Native,
        buyAvailable: Boolean = false
    ) = Chain.Asset(
        icon = null,
        id = 0,
        priceId = null,
        chainId = chainId,
        symbol = symbol.asTokenSymbol(),
        precision = 9.asPrecision(),
        buyProviders = if (buyAvailable) mapOf("provider" to emptyMap()) else emptyMap(),
        sellProviders = emptyMap(),
        staking = emptyList(),
        type = type,
        source = Chain.Asset.Source.DEFAULT,
        name = symbol
    )
}
