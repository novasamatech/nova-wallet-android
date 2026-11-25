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
        assertSiteKind(DAppStakingSiteKind.BITTENSOR, DappSearchResult.Url("https://taostats.io", isTrustedByNova = true))
        assertSiteKind(DAppStakingSiteKind.BITTENSOR, DappSearchResult.Url("https://taoyield.com/pools?id=1", isTrustedByNova = false))
    }

    @Test
    fun `TAO related host wins over the staking keyword`() {
        assertSiteKind(DAppStakingSiteKind.BITTENSOR, DappSearchResult.Url("https://staking.taostats.io", isTrustedByNova = false))
    }

    @Test
    fun `TAO related host wins over the catalogue staking category`() {
        assertSiteKind(DAppStakingSiteKind.BITTENSOR, DappSearchResult.Dapp(dApp("https://tao.app")))
        assertSiteKind(DAppStakingSiteKind.BITTENSOR, DappSearchResult.Url("https://www.tao.app", isTrustedByNova = true))
    }

    @Test
    fun `free text query never shows a notice`() {
        assertSiteKind(DAppStakingSiteKind.NONE, DappSearchResult.Search("tao", "https://duckduckgo.com/?q=tao"))
        assertSiteKind(DAppStakingSiteKind.NONE, DappSearchResult.Search("staking", "https://duckduckgo.com/?q=staking"))
    }

    @Test
    fun `staking keyword host keeps the third party staking notice`() {
        assertSiteKind(DAppStakingSiteKind.THIRD_PARTY_STAKING, DappSearchResult.Url("https://staking.polkadot.cloud", isTrustedByNova = false))
    }

    @Test
    fun `catalogue staking dApp keeps the third party staking notice`() {
        assertSiteKind(DAppStakingSiteKind.THIRD_PARTY_STAKING, DappSearchResult.Dapp(dApp("https://app.bifrost.io")))
        assertSiteKind(DAppStakingSiteKind.THIRD_PARTY_STAKING, DappSearchResult.Url("https://APP.bifrost.io/vstaking", isTrustedByNova = true))
    }

    @Test
    fun `other sites show no notice`() {
        assertSiteKind(DAppStakingSiteKind.NONE, DappSearchResult.Dapp(dApp("https://app.hydration.net")))
        assertSiteKind(DAppStakingSiteKind.NONE, DappSearchResult.Url("https://taobao.com", isTrustedByNova = false))
        assertSiteKind(DAppStakingSiteKind.NONE, DappSearchResult.Url("https://tensor.trade", isTrustedByNova = false))
    }

    private fun assertSiteKind(expected: DAppStakingSiteKind, result: DappSearchResult) = runBlocking {
        assertEquals(expected, interactor.stakingSiteKind(result))
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
