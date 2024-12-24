package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.feature_dapp_api.data.model.DApp

sealed interface DappSearchResult {

    val isTrustedByNova: Boolean

    class Url(val url: String, override val isTrustedByNova: Boolean) : DappSearchResult

    class Search(val query: String, val searchUrl: String) : DappSearchResult {
        override val isTrustedByNova: Boolean = false
    }

    class Dapp(val dapp: DApp) : DappSearchResult {
        override val isTrustedByNova: Boolean = true
    }
}
