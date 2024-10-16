package io.novafoundation.nova.feature_swap_core_api.data.paths.model

class BestPathQuote<E>(
    val candidates: List<QuotedPath<E>>
) {

    val bestPath : QuotedPath<E> = candidates.max()
}
