package io.novafoundation.nova.common.utils.search

class SearchFilter<T>(
    private val query: String,
    private val phraseSearch: PhraseSearch?,
    private val extractors: List<(T) -> String?>
) {

    class Builder<T>(private val query: String, extractor: (T) -> String?) {

        private val extractors = mutableListOf(extractor)
        private var phraseSearch: PhraseSearch? = null

        fun addPhraseSearch(phraseSearch: PhraseSearch): Builder<T> {
            this.phraseSearch = phraseSearch
            return this
        }

        fun or(extractor: (T) -> String?): Builder<T> {
            extractors += extractor
            return this
        }

        fun build(): SearchFilter<T> {
            return SearchFilter(query, phraseSearch, extractors)
        }
    }

    fun filter(value: T): Boolean {
        return extractors.any {
            val extractedValue = it(value) ?: return@any false

            if (phraseSearch != null && phraseSearch.isPhraseQuery()) {
                phraseSearch.matchedWith(extractedValue)
            } else {
                extractedValue.contains(query)
            }
        }
    }
}
