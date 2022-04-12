package io.novafoundation.nova.feature_dapp_impl.presentation.search.model

import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult

class DappSearchModel(
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    @DrawableRes val actionIcon: Int?,
    val searchResult: DappSearchResult
)
