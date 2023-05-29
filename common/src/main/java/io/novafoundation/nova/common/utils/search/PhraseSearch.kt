package io.novafoundation.nova.common.utils.search

interface PhraseSearch {
    fun isPhraseQuery(): Boolean

    fun matchedWith(raw: String): Boolean
}

class CachedPhraseSearch(query: String) : PhraseSearch {

    private val isPhrase = query.trim().contains(' ')
    private val phraseRegex = query.getPhraseRegex()
    private val searchCache = mutableMapOf<String, Boolean>()

    override fun isPhraseQuery(): Boolean {
        return isPhrase
    }

    override fun matchedWith(raw: String): Boolean {
        return if (searchCache.containsKey(raw)) {
            searchCache.getValue(raw)
        } else {
            val matchingResult = phraseRegex.containsMatchIn(raw)
            searchCache[raw] = matchingResult
            matchingResult
        }
    }
}

private fun String.getPhraseRegex(): Regex {
    if (isEmpty()) return "\$.".toRegex() // Regex that doesn't match anything

    return this.trim()
        .replace(" ", ".*")
        .toRegex() // Matching words in order: first.*second.*third
}
