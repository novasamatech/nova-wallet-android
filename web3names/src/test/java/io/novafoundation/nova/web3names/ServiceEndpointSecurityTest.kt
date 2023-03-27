package io.novafoundation.nova.web3names

import io.ipfs.multibase.Multibase
import io.ipfs.multibase.binary.Base64
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.hash.Hasher.blake2b256
import junit.framework.Assert.assertEquals
import org.junit.Test

class ServiceEndpointSecurityTest {

    @Test
    fun check2() {
        val resource = testResource()
        val expectedEncodedHash = "Ug10blEje4DrZsqVV7AfyUKoJ93Jgk8xkvxJkehTmO8o="
        val expectedHash = Multibase.decode(expectedEncodedHash)

        val resourceHash = Base64.encodeBase64(resource.toByteArray()).blake2b256()

        assertEquals(expectedHash.toHexString(), resourceHash.toHexString())
    }

    private fun testResource(): String {
        return """
{
  "polkadot:411f057b9107718c9624d6aa4a3f23c1/slip44:2086": [
    {
      "account": "4qBSZdEoUxPVnUqbX8fjXovgtQXcHK7ZvSf56527XcDZUukq",
      "description": "Treasury proposals transfers"
    },
    {
      "account": "4oHvgA54py7SWFPpBCoubAajYrxj6xyc8yzHiAVryeAq574G",
      "description": "Regular transfers"
    },
    {
      "account": "4taHgf8x9U5b8oJaiYoNEh61jaHpKs9caUdattxfBRkJMHvm"
    }
  ],
  "eip:1/slip44:60": [
    {
      "account": "0x8f8221AFBB33998D8584A2B05749BA73C37A938A",
      "description": "NFT sales"
    },
    {
      "account": "0x6b175474e89094c44da98b954eedeac495271d0f"
    }
  ]
}
        """.trimIndent()
    }
}
