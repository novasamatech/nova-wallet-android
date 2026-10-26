package io.novafoundation.nova.infrastructure.attestation.profile3

import android.content.SharedPreferences
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning
import java.io.IOException
import java.util.UUID
import okio.ByteString.Companion.toByteString

class Profile3Identity(val scope: Profile3TokenScope, val registered: Boolean, val keyCreated: Boolean = registered) {

    val keyAlias: String = "nova.attestation.profile3." +
        AttestationSigning.sha256(scope.origin.toByteArray()).toByteString().hex() + "." + scope.clientId

    override fun toString(): String = "Profile3Identity(<redacted>)"
}

interface Profile3IdentityStore {
    fun current(): Profile3Identity?

    fun create(origin: String): Profile3Identity

    fun markRegistered(scope: Profile3TokenScope)

    fun markKeyCreated(scope: Profile3TokenScope)

    fun clear(scope: Profile3TokenScope)
}

class PreferencesProfile3IdentityStore(private val preferences: SharedPreferences) : Profile3IdentityStore {

    override fun current(): Profile3Identity? {
        val origin = preferences.getString("origin", null) ?: return null
        val clientId = preferences.getString("client_id", null) ?: return null
        val scope = Profile3TokenScope(origin, clientId, preferences.getLong("generation", 0))
        return Profile3Identity(scope, preferences.getBoolean("registered", false), preferences.getBoolean("key_created", false))
    }

    override fun create(origin: String): Profile3Identity {
        val generation = preferences.getLong("generation", 0) + 1
        val scope = Profile3TokenScope(origin, UUID.randomUUID().toString(), generation)
        val saved = preferences.edit()
            .putString("origin", origin)
            .putString("client_id", scope.clientId)
            .putLong("generation", generation)
            .putBoolean("registered", false)
            .putBoolean("key_created", false)
            .commit()
        if (!saved) throw IOException("Could not persist profile-3 identity")
        return Profile3Identity(scope, false)
    }

    override fun markRegistered(scope: Profile3TokenScope) {
        if (current()?.scope != scope) throw IOException("Profile-3 identity changed")
        if (!preferences.edit().putBoolean("registered", true).commit()) throw IOException("Could not persist registration")
    }

    override fun markKeyCreated(scope: Profile3TokenScope) {
        if (current()?.scope != scope) throw IOException("Profile-3 identity changed")
        if (!preferences.edit().putBoolean("key_created", true).commit()) throw IOException("Could not persist key creation")
    }

    override fun clear(scope: Profile3TokenScope) {
        if (current()?.scope != scope) return
        if (!preferences.edit().remove("origin").remove("client_id").remove("registered").remove("key_created").commit()) {
            throw IOException("Could not clear profile-3 identity")
        }
    }
}
