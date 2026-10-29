package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The subnet owner publishes its website and repository in the on-chain identity as free text:
 * no scheme, trailing slashes and plain junk all occur. Only http(s) links with a dotted host survive.
 */
object BittensorSubnetLinks {

    private val schemePrefix = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

    private val hostWithPortPrefix = Regex("^[^:/?#]+:[0-9]+(?:[/#?]|$)")

    private val githubHosts = setOf("github.com", "www.github.com")

    fun website(raw: String?): String? = parse(raw)?.toString()

    /** Only `github.com/<owner>[/<repo>]` counts as a repository link; canonical https, no trailing slash. */
    fun github(raw: String?): String? {
        val url = parse(raw) ?: return null
        if (url.host.lowercase() !in githubHosts) return null

        val parts = pathParts(url)
        if (parts.isEmpty() || parts.size > 2) return null

        return "https://github.com/" + parts.joinToString("/")
    }

    fun websiteLabel(url: String): String = url
        .substringAfter("://")
        .trimEnd('/')

    fun githubLabel(url: String): String = url.toHttpUrlOrNull()
        ?.let(::pathParts)
        ?.joinToString("/")
        ?: url

    private fun parse(raw: String?): HttpUrl? {
        val text = raw?.trim()
            ?.takeIf { it.isNotEmpty() && it.none(Char::isWhitespace) }
            ?: return null

        val hasScheme = schemePrefix.containsMatchIn(text) && !hostWithPortPrefix.containsMatchIn(text)
        val withScheme = if (hasScheme) text else "https://$text"

        return withScheme.toHttpUrlOrNull()
            ?.takeIf { it.host.contains('.') && it.username.isEmpty() && it.password.isEmpty() }
    }

    private fun pathParts(url: HttpUrl): List<String> = url.pathSegments.filter { it.isNotEmpty() }
}
