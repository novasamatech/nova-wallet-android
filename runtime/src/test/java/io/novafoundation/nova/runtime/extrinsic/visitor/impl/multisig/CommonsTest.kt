package io.novafoundation.nova.runtime.extrinsic.visitor.impl.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.nodes.multisig.generateMultisigAddress
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import org.junit.Assert.assertArrayEquals
import org.junit.Test


class CommonsTest {

    @Test
    fun shouldGenerateSs58MultisigAddress() {
        shouldGenerateMultisigAddress(
            signatories = listOf(
                "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
                "5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty",
                "5FLSigC9HGRKVhB9FiEo4Y3koPsNmBmLJbpXg2mp1hXcS59Y"
            ),
            threshold = 2,
            expected = "5DjYJStmdZ2rcqXbXGX7TW85JsrW6uG4y9MUcLq2BoPMpRA7",
        )
    }

    @Test
    fun shouldGenerateEvmMultisigAddress() {
        shouldGenerateMultisigAddress(
            signatories = listOf(
                "0xC60eFE26b9b92380D1b2c479472323eC35F0f0aB",
                "0x61d8c5647f4181f2c35996c62a6272967f5739a8",
                "0xaCCaCE4056A930745218328BF086369Fbd61c212"
            ),
            threshold =2,
            expected = "0xb4e55b61678623fd5ece9c24e79d6c0532bee057",
        )
    }

    private fun shouldGenerateMultisigAddress(
        signatories: List<String>,
        threshold: Int,
        expected: String
    ) {
        val accountIds = signatories.map { it.addressToAccountId() }
        val expectedId = expected.addressToAccountId()

        val actual = generateMultisigAddress(accountIds, threshold)
        assertArrayEquals(expectedId.value, actual.value)
    }

    private fun String.addressToAccountId(): AccountIdKey {
        return if (startsWith("0x")) {
            asEthereumAddress().toAccountId().value
        } else {
            toAccountId()
        }.intoKey()
    }
}
