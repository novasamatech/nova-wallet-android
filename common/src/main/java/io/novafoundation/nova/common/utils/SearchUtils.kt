package io.novafoundation.nova.common.utils

fun String.isSeparatedQuery(): Boolean {
    return contains(' ')
}

fun String.getSeparatedQueryRegex(): Regex {
    if (isEmpty()) return "\$.".toRegex() // Regex that doesn't match anything

    return split(' ')
        .joinToString(".*")
        .toRegex() // Matching words in order: "1".*"2".*"3" ...
}

fun <T> Iterable<T>.filterWith(searchFilter: SearchFilter<T>): List<T> {
    return filter { searchFilter.filter(it) }
}

class SearchFilter<T>(
    private val query: String,
    private val separatedWordFilter: Boolean,
    private val extractors: List<(T) -> String?>
) {

    class Builder<T>(private val query: String, extractor: (T) -> String?) {

        private val extractors = mutableListOf(extractor)
        private var separatedWordSearch: Boolean = false

        fun separatedWordSearch(separatedWordSearch: Boolean): Builder<T> {
            this.separatedWordSearch = separatedWordSearch
            return this
        }

        fun and(extractor: (T) -> String?): Builder<T> {
            extractors += extractor
            return this
        }

        fun build(): SearchFilter<T> {
            return SearchFilter(query, separatedWordSearch, extractors)
        }
    }

    private val isSeparatedQuery = query.isSeparatedQuery()
    private val separatedWordRegex = query.getSeparatedQueryRegex()

    fun filter(value: T): Boolean {
        return extractors.any {
            val extractedValue = it(value) ?: return@any false

            if (separatedWordFilter && isSeparatedQuery) {
                separatedWordRegex.containsMatchIn(extractedValue)
            } else {
                extractedValue.contains(query)
            }
        }
    }
}

class SearchComparator<T>(
    private val query: String,
    private val separatedWordSearch: Boolean,
    private val extractors: List<(T) -> String?>
) : Comparator<T> {

    class Builder<T>(private val query: String, extractor: (T) -> String?) {

        private val extractors = mutableListOf(extractor)
        private var separatedWordSearch: Boolean = false

        fun separatedWordSearch(separatedWordSearch: Boolean): Builder<T> {
            this.separatedWordSearch = separatedWordSearch
            return this
        }

        fun and(extractor: (T) -> String?): Builder<T> {
            extractors += extractor
            return this
        }

        fun build(): SearchComparator<T> {
            return SearchComparator(query, separatedWordSearch, extractors)
        }
    }

    private val isSeparatedQuery = query.isSeparatedQuery()
    private val separatedRegex = query.getSeparatedQueryRegex()

    override fun compare(o1: T, o2: T): Int {
        val o1Scores = extractors.sumOf { it(o1).getMatchingScore() }
        val o2Scores = extractors.sumOf { it(o2).getMatchingScore() }
        return when {
            o1Scores > o2Scores -> -1
            o1Scores < o2Scores -> 1
            else -> 0
        }
    }

    private fun String?.getMatchingScore(): Int {
        if (this == null) return 0

        var score = 0
        if (this == query) {
            score = 1000
        } else if (query in this) {
            score = 1
        } else if (separatedWordSearch && isSeparatedQuery) {
            query.getSeparatedMatchingScore()
        }

        return score
    }

    private fun String.getSeparatedMatchingScore(): Int {
        return if (separatedRegex.containsMatchIn(this)) {
            1
        } else {
            0
        }
    }
}
