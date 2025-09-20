package io.novafoundation.nova.runtime.extrinsic.visitor.impl.derivative

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.derivative.generateDerivativeAddress
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import org.junit.Assert.assertArrayEquals
import org.junit.Test


class CommonsTest {

    @Test
    fun shouldGenerateSs58MultisigAddress() {
        shouldGenerateDerivativeAddress(
            parent = "15UHvPeMjYLvMLqh6bWLxAP3MbqjjsMXFWToJKCijzGPM3p9",
            index = 0,
            expected = "127zarPDhVzmCXVQ7Kfr1yyaa9wsMuJ74GJW9Q7ezHfQEgh6"
        )
    }

    @Test
    fun shouldGenerateEvmMultisigAddress() {
        shouldGenerateDerivativeAddress(
            parent = "0xa781885b344F538c51dc457Acb815F3F94031710",
            index = 0,
            expected = "0x707783019c5a60015384813cd956B5B081CB18a6",
        )
    }

    private fun shouldGenerateDerivativeAddress(
        parent: String,
        index: Int,
        expected: String
    ) {
        val expectedId = expected.addressToAccountId()

        val actual = generateDerivativeAddress(parent.addressToAccountId(), index)
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
