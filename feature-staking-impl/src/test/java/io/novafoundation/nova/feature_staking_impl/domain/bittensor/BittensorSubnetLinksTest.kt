package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorSubnetLinksTest {

    @Test
    fun `website keeps https and http urls`() {
        assertEquals("https://engy.ai/", BittensorSubnetLinks.website("https://engy.ai"))
        assertEquals("http://hotfloat.io/", BittensorSubnetLinks.website("http://hotfloat.io"))
    }

    @Test
    fun `website gets https when the scheme is missing`() {
        assertEquals("https://blockmachine.io/", BittensorSubnetLinks.website("blockmachine.io"))
        assertEquals("https://www.affine.io/", BittensorSubnetLinks.website("  www.affine.io \n"))
        assertEquals("https://byteleap.ai/", BittensorSubnetLinks.website("ByteLeap.ai"))
    }

    @Test
    fun `schemeless hosts can specify a port without becoming URL schemes`() {
        assertEquals("https://example.com:8080/", BittensorSubnetLinks.website("example.com:8080"))
        assertEquals("https://example.com:8080/app?view=1#details", BittensorSubnetLinks.website("example.com:8080/app?view=1#details"))
        assertEquals("https://github.com/owner/repo", BittensorSubnetLinks.github("github.com:443/owner/repo"))
    }

    @Test
    fun `port support does not admit invalid ports unsupported schemes or credentials`() {
        listOf(
            "example.com:65536",
            "example.com:invalid",
            "custom.scheme://example.com",
            "ftp://example.com:8080",
            "user:password@example.com:8080"
        ).forEach { assertNull(it, BittensorSubnetLinks.website(it)) }
    }

    @Test
    fun `website rejects text that is not an http url`() {
        listOf(null, "", "   ", "not a url", "ByteLeap", "ftp://files.example", "https://", "mailto:hi@example.com", "https://user:pw@example.com")
            .forEach { assertNull(it, BittensorSubnetLinks.website(it)) }
    }

    @Test
    fun `github accepts owner and repository only`() {
        assertEquals("https://github.com/hanlinai/engy", BittensorSubnetLinks.github("https://github.com/hanlinai/engy"))
        assertEquals("https://github.com/taostat/blockmachine", BittensorSubnetLinks.github("https://github.com/taostat/blockmachine/"))
        assertEquals("https://github.com/macrocosm-os", BittensorSubnetLinks.github("github.com/macrocosm-os"))
        assertEquals("https://github.com/a/b", BittensorSubnetLinks.github("http://www.github.com/a/b"))
    }

    @Test
    fun `github rejects other hosts and deep paths`() {
        listOf(null, "", "https://gitlab.com/a/b", "https://github.com", "https://github.com/a/b/tree/main", "a/b")
            .forEach { assertNull(it, BittensorSubnetLinks.github(it)) }
    }

    @Test
    fun `labels drop the scheme and the trailing slash`() {
        assertEquals("chutes.ai", BittensorSubnetLinks.websiteLabel("https://chutes.ai/"))
        assertEquals("tag101.ai/app", BittensorSubnetLinks.websiteLabel("http://tag101.ai/app"))
        assertEquals("chutesai/chutes", BittensorSubnetLinks.githubLabel("https://github.com/chutesai/chutes"))
    }
}
