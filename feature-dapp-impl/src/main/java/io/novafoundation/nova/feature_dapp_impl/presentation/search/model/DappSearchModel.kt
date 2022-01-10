package io.novafoundation.nova.feature_dapp_impl.presentation.search.model

import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult

class DappSearchModel(
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val searchResult: DappSearchResult
)
