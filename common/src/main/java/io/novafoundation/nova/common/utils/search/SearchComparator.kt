package io.novafoundation.nova.common.utils.search

class SearchComparator<T>(
    private val query: String,
    private val phraseSearch: PhraseSearch?,
    private val extractors: List<(T) -> String?>
) : Comparator<T> {

    class Builder<T>(private val query: String, extractor: (T) -> String?) {

        private val extractors = mutableListOf(extractor)
        private var phraseSearch: PhraseSearch? = null

        fun addPhraseSearch(phraseSearch: PhraseSearch): Builder<T> {
            this.phraseSearch = phraseSearch
            return this
        }

        fun and(extractor: (T) -> String?): Builder<T> {
            extractors += extractor
            return this
        }

        fun build(): SearchComparator<T> {
            return SearchComparator(query, phraseSearch, extractors)
        }
    }

    override fun compare(o1: T, o2: T): Int {
        val o1Scores = extractors.sumOf { it(o1).getMatchingScore() }
        val o2Scores = extractors.sumOf { it(o2).getMatchingScore() }
        return o1Scores.compareTo(o2Scores)
    }

    private fun String?.getMatchingScore(): Int {
        if (this == null) return 0

        var score = 0
        if (this == query) {
            score = 1000
        } else if (query in this) {
            score = 1
        } else if (phraseSearch != null && phraseSearch.isPhraseQuery()) {
            query.getSeparatedMatchingScore()
        }

        return score
    }

    private fun String.getSeparatedMatchingScore(): Int {
        return if (phraseSearch!!.matchedWith(this)) {
            1
        } else {
            0
        }
    }
}
