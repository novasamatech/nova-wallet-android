package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.common.data.storage.Preferences
import java.util.UUID

private const val PREFS_CLIENT_ID = "attestation_client_id"
private const val PREFS_ATTESTED_CLIENT_ID = "attestation_attested_client_id"

interface AttestationIdentity {

    fun clientId(): String

    fun isAttested(): Boolean

    fun markAttested()

    /** Forgets that the current id is registered while keeping the id itself - the next use registers it again. */
    fun clearAttested()

    /**
     * Forgets the current client id so the next [clientId] mints a new one.
     *
     * Re-registering under the same id is not an option: the backend refuses to rebind an id to a
     * different key, so recovering from a lost or mismatched binding means a new identity.
     */
    fun reset()
}

class RealAttestationIdentity(
    private val preferences: Preferences
) : AttestationIdentity {

    override fun clientId(): String {
        preferences.getString(PREFS_CLIENT_ID)?.let { return it }

        val generated = UUID.randomUUID().toString()
        preferences.putString(PREFS_CLIENT_ID, generated)
        return generated
    }

    override fun isAttested(): Boolean = preferences.getString(PREFS_ATTESTED_CLIENT_ID) == clientId()

    override fun markAttested() = preferences.putString(PREFS_ATTESTED_CLIENT_ID, clientId())

    override fun clearAttested() = preferences.removeField(PREFS_ATTESTED_CLIENT_ID)

    override fun reset() {
        preferences.removeField(PREFS_ATTESTED_CLIENT_ID)
        preferences.removeField(PREFS_CLIENT_ID)
    }
}
