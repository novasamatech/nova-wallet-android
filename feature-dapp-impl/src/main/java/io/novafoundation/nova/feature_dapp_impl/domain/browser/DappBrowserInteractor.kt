package io.novafoundation.nova.feature_dapp_impl.domain.browser

import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.util.Urls
import io.novafoundation.nova.feature_dapp_impl.util.isSecure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URL

class DappBrowserInteractor(
    private val phishingSitesRepository: PhishingSitesRepository,
    private val favouritesDAppRepository: FavouritesDAppRepository,
) {

    suspend fun removeDAppFromFavourites(dAppUrl: String) {
        favouritesDAppRepository.removeFavourite(dAppUrl)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun observeBrowserPageFor(browserPage: BrowserPage): Flow<BrowserPageAnalyzed> {
        return favouritesDAppRepository.observeIsFavourite(browserPage.url).map { isFavourite ->
            runCatching {
                val security = when {
                    phishingSitesRepository.isPhishing(browserPage.url) -> BrowserPageAnalyzed.Security.DANGEROUS
                    URL(browserPage.url).isSecure -> BrowserPageAnalyzed.Security.SECURE
                    else -> BrowserPageAnalyzed.Security.UNKNOWN
                }
                BrowserPageAnalyzed(
                    display = Urls.hostOf(browserPage.url),
                    title = browserPage.title,
                    url = browserPage.url,
                    security = security,
                    isFavourite = isFavourite,
                    synchronizedWithBrowser = browserPage.synchronizedWithBrowser
                )
            }.getOrElse {
                BrowserPageAnalyzed(
                    display = browserPage.url,
                    title = browserPage.title,
                    url = browserPage.url,
                    isFavourite = isFavourite,
                    security = BrowserPageAnalyzed.Security.UNKNOWN,
                    synchronizedWithBrowser = browserPage.synchronizedWithBrowser
                )
            }
        }
    }
}
