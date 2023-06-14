package io.novafoundation.nova.web3names

import com.google.gson.Gson
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.serviceEndpoint.ServiceEndpoint
import io.novafoundation.nova.web3names.data.serviceEndpoint.W3NServiceEndpointHandlerV1
import io.novafoundation.nova.web3names.data.serviceEndpoint.W3NServiceEndpointHandlerV2
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock


class We3NamesIntegrityVerifierTestV2 {

    private val endpointHandler = W3NServiceEndpointHandlerV2(
        endpoint = mock(ServiceEndpoint::class.java),
        transferRecipientApi = mock(TransferRecipientsApi::class.java),
        gson = Gson()
    )

    @Test
    fun `should accept valid id matching resource hash`() = runTest(
        endpointId = "UyLezITywEzmapmqriDu7sQM9xfXefYAqlmLxVoHFLpw=",
        expectedOutcome = true,
    )

    @Test
    fun `should reject valid id not matching resource hash`() {
        runTest(
            endpointId = "Uinvalid-hash=",
            expectedOutcome = false,
        )

        runTest(
            endpointId = "UyLezITywEzmapmqriDu7sQM9xfXefYAqlmLxVoHFLpw=",
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
        assertEquals(expectedOutcome, endpointHandler.verifyIntegrity(endpointId, resource))
    }

    private fun testResource(): String {
        return """
{
  "polkadot:91b171bb158e2d3848fa23a9f1c25182/slip44:354": {
    "121eb2BdXbD2AbSno4qSpiGKL28TNGngzvfvLXQtN6qCmG7N": {
      "description": "🏡 Day 7"
    },
    "158rvBQ6sPTM7YMPrSvMMPpuQovELfMQdeFomXQkj63TjeNy": {
      "description": "valid DOT Polkadot"
    },
    "0x467E99717a0f54226454121e6e20Dd1549b30d0e": {},
    "0x3d89f11254b4eb4c251ED2593863b": {
      "description": "Invalid Personal account"
    }
  },
  "polkadot:411f057b9107718c9624d6aa4a3f23c1/slip44:2086": {
    "4q1yjt4UKaArQbLbb43AKHeFhjeEtQZ77j3MFEQDEEpsgYo1": {
      "description": "Valid Kilt address"
    },
    "4nvZhWv71x8reD9gq7BUGYQQVvTiThnLpTTanyru9XckaeWa": {
      "description": "Council account"
    }
  },
  "polkadot:fc41b9bd8ef8fe53d58c7ea67c794c7e/slip44:787": {
    "251TW6p8iJCcg8Q2umib8QV9sfMAtVecX3o7vMqmMdzLYif7": {
      "description": "valid ACA Acala"
    },
    "251TW6p8iJCcg8Q2umib8QV9sfMAtVecX3o7vMqmMdz": {
      "description": "invalid Acala"
    }
  },
  "polkadot:fc41b9bd8ef8fe53d58c7ea67c794c7e/slip44:354": {
    "251TW6p8iJCcg8Q2umib8QV9sfMAtVecX3o7vMqmMdzLYif7": {
      "description": "valid DOT Acala"
    },
    "251TW6p8iJCcg8Q2umib8QV9sfMAtVecX3o7vMqmMdz": {
      "description": "invalid Acala"
    }
  },
  "polkadot:fe58ea77779b7abda7da4ec526d14db9/slip44:1284": {
    "0x467E99717a0f54226454121e6e20Dd1549b30d0e": {
      "description": "valid GLMR Moonbeam"
    },
    "0x467E99717a0f54226454121e6e20Dd1549b": {
      "description": "invalid Moonbeam"
    }
  },
  "polkadot:fe58ea77779b7abda7da4ec526d14db9/slip44:354": {
    "0x467E99717a0f54226454121e6e20Dd1549b30d0e": {
      "description": "valid DOT Moonbeam"
    },
    "0x467E99717a0f54226454121e6e20Dd1549b": {
      "description": "invalid Moonbeam"
    }
  },
  "polkadot:b0a8d493285c2df73290dfb7e61f870f/slip44:434": {
    "Day71GSJAxUUiFic8bVaWoAczR3Ue3jNonBZthVHp2BKzyJ": {
      "description": "🏡 Day 7"
    }
  },
  "eip155:1/slip44:60": {
    "0x5173Dab1947CFD36fA7dCdBAE55C7E71A982a181": {
      "description": "valid ETH Ethereum"
    },
    "5173Dab1947CFD36fA7dCdBAE55C7E71A982a181": {
      "description": "invalid ETH"
    }
  },
  "eip155:1/erc20:0x6b175474e89094c44da98b954eedeac495271d0f": {
    "0x5173Dab1947CFD36fA7dCdBAE55C7E71A982a181": {
      "description": "valid DAI Ethereum"
    },
    "0x6b175474e89094c44da98b954ee": {}
  }
}
        """.trimIndent()
    }
}
