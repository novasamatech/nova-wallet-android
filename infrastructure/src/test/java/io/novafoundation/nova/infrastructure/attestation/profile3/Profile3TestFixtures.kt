package io.novafoundation.nova.infrastructure.attestation.profile3

import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.AttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.IntegrityTokenSource
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

internal class TestTokenClock : Profile3TokenClock {
    var now = 1_000L

    var boot = 7

    override fun elapsedRealtimeMillis() = now

    override fun bootCount() = boot
}

internal class TestIdentityStore : Profile3IdentityStore {
    var identity: Profile3Identity? = null

    var generations = 0L

    override fun current() = identity

    override fun create(origin: String): Profile3Identity {
        val scope = Profile3TokenScope(origin, "00000000-0000-4000-8000-${(++generations).toString().padStart(12, '0')}", generations)
        return Profile3Identity(scope, false).also { identity = it }
    }

    override fun markRegistered(scope: Profile3TokenScope) {
        check(identity?.scope == scope)
        identity = Profile3Identity(scope, true)
    }

    override fun markKeyCreated(scope: Profile3TokenScope) {
        check(identity?.scope == scope)
        identity = Profile3Identity(scope, identity!!.registered, true)
    }

    override fun clear(scope: Profile3TokenScope) {
        if (identity?.scope == scope) identity = null
    }
}

internal class TestKeys : AttestationKeyPairStore {
    val aliases = mutableSetOf<String>()

    override fun contains(alias: String) = alias in aliases

    override fun publicKey(alias: String): ByteArray {
        aliases += alias
        return byteArrayOf(1, 2, 3)
    }

    override fun sign(alias: String, payload: ByteArray): ByteArray {
        check(alias in aliases)
        return byteArrayOf(4, 5, 6)
    }

    override fun delete(alias: String) {
        aliases -= alias
    }
}

internal class TestTokenStore : Profile3TokenStore {
    val records = mutableMapOf<Profile3TokenScope, Profile3TokenRecord>()

    var failWrite = false

    override suspend fun read(scope: Profile3TokenScope) = records[scope]

    override suspend fun write(record: Profile3TokenRecord) {
        if (failWrite) throw IOException("Disk unavailable")
        records[record.scope] = record
    }

    override suspend fun remove(scope: Profile3TokenScope) {
        records.remove(scope)
    }

    override suspend fun removeIfMatches(scope: Profile3TokenScope, accessToken: String): Boolean {
        if (records[scope]?.accessToken != accessToken) return false
        records.remove(scope)
        return true
    }
}

internal class TestProfile3Api : Profile3Api {
    val challenges = mutableListOf<Profile3ChallengeRequest>()

    val registrationRequests = mutableListOf<Profile3RegisterRequest>()

    val tokenRequests = mutableListOf<Profile3TokenRequest>()

    val destinations = mutableListOf<String>()

    var beforeToken: suspend () -> Unit = {}

    var failure: Exception? = null

    var registerStatus = 204

    var knownClient = true

    override suspend fun challenge(url: String, request: Profile3ChallengeRequest): Profile3ChallengeResponse {
        destinations += url
        challenges += request
        if (request.purpose == "token" && !knownClient) throw httpFailure(401, "unknown_client")
        return Profile3ChallengeResponse("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8", 1, 61)
    }

    override suspend fun register(url: String, request: Profile3RegisterRequest): Response<Unit> {
        destinations += url
        registrationRequests += request
        knownClient = true
        return if (registerStatus == 204) Response.success<Unit>(204, null) else Response.success(Unit)
    }

    override suspend fun token(url: String, request: Profile3TokenRequest): Profile3TokenResponse {
        destinations += url
        tokenRequests += request
        beforeToken()
        failure?.let { throw it }
        return Profile3TokenResponse("opaque-token-${tokenRequests.size}", 1_800_000_000, 1_800_000_300)
    }
}

internal fun httpFailure(status: Int, code: String, retryAfter: String? = null): HttpException {
    val body = "{\"error\":{\"code\":\"$code\"}}".toResponseBody("application/json".toMediaType())
    val headers = okhttp3.Headers.Builder().apply { if (retryAfter != null) add("Retry-After", retryAfter) }.build()
    val raw = okhttp3.Response.Builder()
        .request(okhttp3.Request.Builder().url("https://infra.test/").build())
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(status)
        .message("test")
        .headers(headers)
        .build()
    return HttpException(Response.error<Unit>(body, raw))
}

internal class Profile3Fixture(testScope: CoroutineScope) {
    private val ownerJob = SupervisorJob(testScope.coroutineContext[Job])

    private val ownerScope = CoroutineScope(testScope.coroutineContext + ownerJob)

    val clock = TestTokenClock()

    val identities = TestIdentityStore()

    val keys = TestKeys()

    val store = TestTokenStore()

    val api = TestProfile3Api()

    var configuredOrigin = "https://infra.test/".toHttpUrl()

    val urls = InfrastructureUrls { configuredOrigin }

    val random = Profile3TokenRandom { 0.0 }

    val factory = Profile3TokenRecordFactory(clock, random)

    var integrityFailure: Exception? = null

    val integrity = IntegrityTokenSource { integrityFailure?.let { throw it } ?: "synthetic-integrity-proof" }

    val registration = Profile3RegistrationService(api, keys, integrity, "io.novafoundation.nova.debug", urls, identities)

    fun provider() = RealProfile3AccessTokenProvider(api, registration, identities, keys, store, factory, clock, random, urls, ownerScope)
}
