package io.novafoundation.nova.infrastructure.attestation.profile3

import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class Profile3TokenStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val clock = TestTokenClock()

    private val keyGenerator = KeyGenerator.getInstance("AES").apply { init(256) }

    private val secret = keyGenerator.generateKey()

    private val key = object : Profile3TokenEncryptionKey {
        override fun getOrCreate(): SecretKey = secret
        override fun existing(): SecretKey = secret
    }

    private val cipher = AesGcmProfile3TokenCipher(key)

    private val random = Profile3TokenRandom { 0.0 }

    private val factory = Profile3TokenRecordFactory(clock, random)

    private val scope = Profile3TokenScope("https://infra.test", "client", 1)

    @Test
    fun `encrypted file is reusable in same boot and contains no plaintext bearer`() = runTest {
        val directory = folder.newFolder()
        val store = EncryptedProfile3TokenStore(directory, cipher, clock)
        val record = factory.create(scope, "synthetic-sensitive-bearer", 1, 301, clock.now, clock.boot)
        store.write(record)
        val file = directory.listFiles()!!.single()
        assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains(record.accessToken))
        val restarted = EncryptedProfile3TokenStore(directory, cipher, clock)
        val restored = restarted.read(scope)!!
        assertEquals(record.accessToken, restored.accessToken)
        assertEquals(record.deadlineElapsedRealtimeMillis, restored.deadlineElapsedRealtimeMillis)
        assertEquals(record.earlyRefreshWindowSeconds, restored.earlyRefreshWindowSeconds)
    }

    @Test
    fun `reboot expiry corrupt ciphertext and mismatched associated scope delete unusable records`() = runTest {
        val directory = folder.newFolder()
        val store = EncryptedProfile3TokenStore(directory, cipher, clock)
        val record = factory.create(scope, "opaque", 1, 301, clock.now, clock.boot)
        store.write(record)
        clock.boot += 1
        assertNull(store.read(scope))
        assertTrue(directory.listFiles()!!.isEmpty())
        clock.boot -= 1
        store.write(record)
        clock.now = record.deadlineElapsedRealtimeMillis
        assertNull(store.read(scope))
        clock.now = 1_000
        store.write(record)
        val file = directory.listFiles()!!.single()
        val tampered = file.readBytes()
        tampered[tampered.lastIndex] = (tampered.last().toInt() xor 1).toByte()
        file.writeBytes(tampered)
        assertNull(store.read(scope))
        assertTrue(directory.listFiles()!!.isEmpty())
    }

    @Test
    fun `scope isolation and compare removal preserve a newer bearer`() = runTest {
        val directory = folder.newFolder()
        val store = EncryptedProfile3TokenStore(directory, cipher, clock)
        val old = factory.create(scope, "old", 1, 301, clock.now, clock.boot)
        val replacement = factory.create(scope, "new", 1, 301, clock.now, clock.boot)
        store.write(old)
        assertNull(store.read(scope.copy(origin = "https://other.test")))
        store.write(replacement)
        assertFalse(store.removeIfMatches(scope, old.accessToken))
        assertEquals("new", store.read(scope)?.accessToken)
        assertTrue(store.removeIfMatches(scope, replacement.accessToken))
        assertNull(store.read(scope))
    }

    @Test
    fun `failed directory creation never falls back to plaintext or memory only`() = runTest {
        val notDirectory: File = folder.newFile()
        val store = EncryptedProfile3TokenStore(notDirectory, cipher, clock)
        val record = factory.create(scope, "opaque", 1, 301, clock.now, clock.boot)
        assertTrue(runCatching { store.write(record) }.isFailure)
        assertEquals(0, notDirectory.length())
    }
}
