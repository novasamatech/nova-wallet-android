package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCatalog
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SearchDappInteractorTest {

    private val stakingCategory = DappCategory(iconUrl = null, name = "Staking", id = "staking")

    private val defiCategory = DappCategory(iconUrl = null, name = "DeFi", id = "defi")

    private val catalog = DappCatalog(
        popular = emptyList(),
        categories = listOf(stakingCategory, defiCategory),
        dApps = listOf(
            dAppMetadata("https://tao.app", stakingCategory),
            dAppMetadata("https://app.bifrost.io", stakingCategory),
            dAppMetadata("https://app.hydration.net", defiCategory)
        )
    )

    private val dAppMetadataRepository = mock(DAppMetadataRepository::class.java)

    private val favouritesDAppRepository = mock(FavouritesDAppRepository::class.java)

    private val interactor = SearchDappInteractor(dAppMetadataRepository, favouritesDAppRepository)

    @Before
    fun setup() = runBlocking<Unit> {
        `when`(dAppMetadataRepository.getDAppCatalog()).thenReturn(catalog)
    }

    @Test
    fun `TAO related address shows the Bittensor notice`() {
        assertSiteKind(DAppStakingKind.BITTENSOR, DappSearchResult.Url("https://taostats.io", isTrustedByNova = true))
        assertSiteKind(DAppStakingKind.BITTENSOR, DappSearchResult.Url("https://taoyield.com/pools?id=1", isTrustedByNova = false))
    }

    @Test
    fun `TAO related host wins over the staking keyword`() {
        assertSiteKind(DAppStakingKind.BITTENSOR, DappSearchResult.Url("https://staking.taostats.io", isTrustedByNova = false))
    }

    @Test
    fun `TAO related host wins over the catalogue staking category`() {
        assertSiteKind(DAppStakingKind.BITTENSOR, DappSearchResult.Dapp(dApp("https://tao.app")))
        assertSiteKind(DAppStakingKind.BITTENSOR, DappSearchResult.Url("https://www.tao.app", isTrustedByNova = true))
    }

    @Test
    fun `free text query never shows a notice`() {
        assertSiteKind(null, DappSearchResult.Search("tao", "https://duckduckgo.com/?q=tao"))
        assertSiteKind(null, DappSearchResult.Search("staking", "https://duckduckgo.com/?q=staking"))
    }

    @Test
    fun `staking keyword host keeps the third party staking notice`() {
        assertSiteKind(DAppStakingKind.STAKING, DappSearchResult.Url("https://staking.polkadot.cloud", isTrustedByNova = false))
    }

    @Test
    fun `catalogue staking dApp keeps the third party staking notice`() {
        assertSiteKind(DAppStakingKind.STAKING, DappSearchResult.Dapp(dApp("https://app.bifrost.io")))
        assertSiteKind(DAppStakingKind.STAKING, DappSearchResult.Url("https://APP.bifrost.io/vstaking", isTrustedByNova = true))
    }

    @Test
    fun `other sites show no notice`() {
        assertSiteKind(null, DappSearchResult.Dapp(dApp("https://app.hydration.net")))
        assertSiteKind(null, DappSearchResult.Url("https://taobao.com", isTrustedByNova = false))
        assertSiteKind(null, DappSearchResult.Url("https://tensor.trade", isTrustedByNova = false))
    }

    @Test
    fun `resolved host excludes the port and path`() = runBlocking {
        val result = DappSearchResult.Url("https://taostats.io:8443/dashboard", isTrustedByNova = true)

        val details = interactor.resolveSearchResult(result)

        assertEquals("taostats.io", details.host)
        assertEquals(DAppStakingKind.BITTENSOR, details.stakingKind)
    }

    private fun assertSiteKind(expected: DAppStakingKind?, result: DappSearchResult) = runBlocking {
        assertEquals(expected, interactor.resolveSearchResult(result).stakingKind)
    }

    private fun dAppMetadata(url: String, category: DappCategory) = DappMetadata(
        name = url,
        iconLink = "",
        url = url,
        baseUrl = url,
        categories = setOf(category)
    )

    private fun dApp(url: String) = DApp(
        name = url,
        description = "",
        iconLink = null,
        url = url,
        isFavourite = false,
        favoriteIndex = null
    )
}
