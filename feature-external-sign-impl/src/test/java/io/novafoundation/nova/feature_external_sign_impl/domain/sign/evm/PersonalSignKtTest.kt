package io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.web3j.crypto.Sign

class PersonalSignKtTest {

    @Test
    fun `should perform personal sign`() {
        val message = "Test".encodeToByteArray()
        val expected = expectedSign(message)
        val actual = message.asEthereumPersonalSignMessage()

        assertArrayEquals(expected, actual)
    }

    // call to reference package-private implementation via reflection.
    // It is not good approach to use this as our actual implementation since reflection is unsafe and slow
    private fun expectedSign(message: ByteArray): ByteArray {
        val method = Sign::class.java.getDeclaredMethod("getEthereumMessageHash", ByteArray::class.java)
        method.isAccessible = true

        return method.invoke(null, message) as ByteArray
    }
}
