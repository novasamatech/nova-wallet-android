package io.novafoundation.nova.web3names

import io.novafoundation.nova.web3names.data.integrity.RealWe3NamesIntegrityVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

class We3NamesIntegrityVerifierTest {
    
    private val integrityVerifier = RealWe3NamesIntegrityVerifier()

    @Test
    fun `should accept valid id matching resource hash`() = runTest(
        endpointId = "Uc1JU0UF9iDfjaRkgHCFG2Rc5jki-cuhlgbEQcjN6-g0=",
        expectedOutcome = true,
    )

    @Test
    fun `should reject valid id not matching resource hash`() {
        runTest(
            endpointId = "Uinvalid-hash=",
            expectedOutcome = false,
        )

        runTest(
            endpointId = "Uc1JU0UF9iDfjaRkgHCFG2Rc5jki-cuhlgbEQcjN6-g0=",
            resource = "changed resource",
            expectedOutcome = false,
        )
    }

    @Test
    fun `should reject invalid id`() = runTest(
        endpointId = "123",
        expectedOutcome = false,
    )

    private fun runTest(
        endpointId: String,
        resource: String = testResource(),
        expectedOutcome: Boolean
    ) {
        assertEquals(expectedOutcome, integrityVerifier.verifyIntegrity(endpointId, resource))
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
