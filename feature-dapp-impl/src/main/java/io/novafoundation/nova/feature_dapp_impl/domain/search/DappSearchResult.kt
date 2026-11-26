package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.feature_dapp_api.data.model.DApp

sealed interface DappSearchResult {

    val targetUrl: String

    val isTrustedByNova: Boolean

    class Url(val url: String, override val isTrustedByNova: Boolean) : DappSearchResult {
        override val targetUrl: String
            get() = url
    }

    class Search(val query: String, val searchUrl: String) : DappSearchResult {
        override val targetUrl: String
            get() = searchUrl

        override val isTrustedByNova: Boolean = false
    }

    class Dapp(val dapp: DApp) : DappSearchResult {
        override val targetUrl: String
            get() = dapp.url

        override val isTrustedByNova: Boolean = true
    }
}
