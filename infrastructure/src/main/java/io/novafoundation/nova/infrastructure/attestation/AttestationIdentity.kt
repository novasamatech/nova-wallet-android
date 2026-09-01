package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.common.data.storage.Preferences
import java.util.UUID

private const val PREFS_CLIENT_ID = "attestation_client_id"
private const val PREFS_ATTESTED_CLIENT_ID = "attestation_attested_client_id"

interface AttestationIdentity {

    fun clientId(): String

    fun isAttested(): Boolean

    fun markAttested()
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
}
