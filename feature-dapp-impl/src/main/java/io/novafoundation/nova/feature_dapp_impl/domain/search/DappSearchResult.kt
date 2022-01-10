package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata

sealed class DappSearchResult {

    class Url(val url: String): DappSearchResult()

    class Search(val query: String): DappSearchResult()

    class Dapp(val metadata: DappMetadata): DappSearchResult()
}
