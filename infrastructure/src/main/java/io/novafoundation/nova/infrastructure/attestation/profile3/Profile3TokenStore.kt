package io.novafoundation.nova.infrastructure.attestation.profile3

import io.novafoundation.nova.infrastructure.attestation.AttestationSigning
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.toByteString

interface Profile3TokenStore {
    suspend fun read(scope: Profile3TokenScope): Profile3TokenRecord?

    suspend fun write(record: Profile3TokenRecord)

    suspend fun remove(scope: Profile3TokenScope)

    suspend fun removeIfMatches(scope: Profile3TokenScope, accessToken: String): Boolean
}

class EncryptedProfile3TokenStore(
    private val directory: File,
    private val cipher: Profile3TokenCipher,
    private val clock: Profile3TokenClock
) : Profile3TokenStore {

    private val mutex = Mutex()

    override suspend fun read(scope: Profile3TokenScope): Profile3TokenRecord? = mutex.withLock { readRecord(scope) }

    override suspend fun write(record: Profile3TokenRecord) = mutex.withLock {
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Could not create token storage")
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use {
            it.writeUTF(record.accessToken)
            it.writeLong(record.issuedAtSeconds)
            it.writeLong(record.expiresAtSeconds)
            it.writeLong(record.issuanceStartElapsedRealtimeMillis)
            it.writeInt(record.bootCount)
            it.writeLong(record.earlyRefreshWindowSeconds)
        }
        val encrypted = cipher.encrypt(bytes.toByteArray(), associatedData(record.scope))
        val destination = file(record.scope)
        val temporary = File.createTempFile("token-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(encrypted)
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) throw IOException("Could not atomically persist token")
        } finally {
            temporary.delete()
        }
    }

    override suspend fun remove(scope: Profile3TokenScope) = mutex.withLock { delete(scope) }

    override suspend fun removeIfMatches(scope: Profile3TokenScope, accessToken: String): Boolean = mutex.withLock {
        if (readRecord(scope)?.accessToken != accessToken) return@withLock false
        delete(scope)
        true
    }

    private fun readRecord(scope: Profile3TokenScope): Profile3TokenRecord? {
        val file = file(scope)
        if (!file.exists()) return null
        return try {
            if (file.length() !in 1..MAX_RECORD_BYTES) throw IOException("Invalid encrypted token size")
            val plaintext = cipher.decrypt(file.readBytes(), associatedData(scope))
            val record = DataInputStream(ByteArrayInputStream(plaintext)).use { input ->
                val result = Profile3TokenRecord(
                    scope,
                    input.readUTF(),
                    input.readLong(),
                    input.readLong(),
                    input.readLong(),
                    input.readInt(),
                    input.readLong()
                )
                if (input.available() != 0) throw IOException("Invalid token record")
                result
            }
            if (!record.isUsable(clock.elapsedRealtimeMillis(), clock.bootCount())) {
                delete(scope)
                null
            } else {
                record
            }
        } catch (failure: Exception) {
            delete(scope)
            null
        }
    }

    private fun delete(scope: Profile3TokenScope) {
        val file = file(scope)
        if (file.exists() && !file.delete()) throw IOException("Could not remove token record")
    }

    private fun associatedData(scope: Profile3TokenScope): ByteArray =
        "${scope.origin}|${scope.clientId}|${scope.generation}".toByteArray(Charsets.UTF_8)

    private fun file(scope: Profile3TokenScope): File {
        val name = AttestationSigning.sha256(associatedData(scope)).toByteString().hex()
        return File(directory, "$name.bin")
    }

    private companion object {
        const val MAX_RECORD_BYTES = 8_192L
    }
}
