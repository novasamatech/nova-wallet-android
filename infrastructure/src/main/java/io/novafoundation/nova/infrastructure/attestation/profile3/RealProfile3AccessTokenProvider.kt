package io.novafoundation.nova.infrastructure.attestation.profile3

import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.AttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.AttestationRefusedException
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning
import io.novafoundation.nova.infrastructure.attestation.AttestationUnavailableException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnsupportedException
import io.novafoundation.nova.infrastructure.attestation.bootstrapRefusal
import io.novafoundation.nova.infrastructure.attestation.isAttestationUnsupported
import io.novafoundation.nova.infrastructure.attestation.peekErrorCode
import io.novafoundation.nova.infrastructure.attestation.signedContext
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import retrofit2.HttpException

class RealProfile3AccessTokenProvider(
    private val api: Profile3Api,
    private val registration: Profile3RegistrationService,
    private val identities: Profile3IdentityStore,
    private val keys: AttestationKeyPairStore,
    private val tokens: Profile3TokenStore,
    private val tokenFactory: Profile3TokenRecordFactory,
    private val clock: Profile3TokenClock,
    private val random: Profile3TokenRandom,
    private val urls: InfrastructureUrls,
    private val ownerScope: CoroutineScope
) : Profile3AccessTokenProvider {

    private class OriginState {
        var token: Profile3TokenRecord? = null

        var issuance: Deferred<Profile3TokenRecord>? = null

        var cooldownUntil = 0L

        var networkCooldownUntil = 0L

        var failures = 0

        // The backend refused this device or client; it is not asked again in this process.
        var refusal: Refusal? = null

        // The last bootstrap failed because this device cannot attest; later refusals keep saying so.
        var unsupported = false
    }

    private class Refusal(val status: Int, val code: String?)

    private class TokenDecision(val usable: Profile3TokenRecord?, val issuance: Deferred<Profile3TokenRecord>?)

    private val mutex = Mutex()

    private val states = mutableMapOf<Profile3TokenScope, OriginState>()

    override suspend fun token(origin: HttpUrl): Profile3TokenRecord {
        requireConfiguredOrigin(urls, origin)
        val decision = mutex.withLock {
            val identity = identityFor(origin)
            val state = states.getOrPut(identity.scope) { OriginState() }
            val now = clock.elapsedRealtimeMillis()
            val current = (state.token ?: tokens.read(identity.scope))?.takeIf { it.isUsable(now, clock.bootCount()) }
            state.token = current
            if (current != null && (!current.shouldRefresh(now) || state.refusal != null || now < state.cooldownUntil)) {
                return@withLock TokenDecision(current, null)
            }
            // Repeats the backend's answer, so callers still tell a refused device from a rejected request.
            state.refusal?.let { throw AttestationRefusedException(it.status, it.code, "Profile-3 registration was refused") }
            if (now < state.cooldownUntil) {
                if (state.unsupported) throw AttestationUnsupportedException("Profile-3 attestation is unsupported on this device")
                throw AttestationUnavailableException("Profile-3 bootstrap is cooling down")
            }
            if (state.issuance == null) {
                val work = ownerScope.async(start = CoroutineStart.LAZY) { loadToken(identity, origin, state) }
                state.issuance = work
                work.start()
            }
            TokenDecision(current, state.issuance)
        }
        val record = decision.usable ?: requireNotNull(decision.issuance).await()
        requireConfiguredOrigin(urls, origin)
        mutex.withLock {
            if (identities.current()?.scope != record.scope) throw IOException("Profile-3 identity changed")
        }
        return record
    }

    override suspend fun invalidate(record: Profile3TokenRecord) = mutex.withLock {
        val state = states[record.scope]
        if (state?.token?.accessToken == record.accessToken) state.token = null
        tokens.removeIfMatches(record.scope, record.accessToken)
        Unit
    }

    override suspend fun recordResponse(record: Profile3TokenRecord, status: Int, code: String?, retryAfter: String?) = mutex.withLock {
        val state = states[record.scope] ?: return@withLock
        if (status == 429) state.networkCooldownUntil = retryDeadline(retryAfter)
        if (status == 503) state.networkCooldownUntil = backoffDeadline(state)
    }

    override suspend fun beforeNetwork(origin: HttpUrl) = mutex.withLock {
        requireConfiguredOrigin(urls, origin)
        val current = identities.current() ?: throw IOException("Profile-3 identity is unavailable")
        if (current.scope.origin != canonicalProfile3Origin(origin)) throw IOException("Profile-3 origin changed")
        val state = states[current.scope] ?: throw IOException("Profile-3 token is unavailable")
        if (clock.elapsedRealtimeMillis() < state.networkCooldownUntil) throw IOException("Profile-3 origin is cooling down")
    }

    private suspend fun identityFor(origin: HttpUrl): Profile3Identity {
        val expectedOrigin = canonicalProfile3Origin(origin)
        val existing = identities.current()
        if (existing != null && (existing.scope.origin != expectedOrigin || existing.keyCreated && !keys.contains(existing.keyAlias))) {
            states.remove(existing.scope)?.issuance?.cancel()
            tokens.remove(existing.scope)
            keys.delete(existing.keyAlias)
            identities.clear(existing.scope)
        }
        return identities.current() ?: identities.create(expectedOrigin)
    }

    private suspend fun loadToken(identity: Profile3Identity, origin: HttpUrl, state: OriginState): Profile3TokenRecord {
        try {
            if (!identity.registered) register(identity, origin)
            val record = issueWithRecovery(identity, origin)
            mutex.withLock {
                requireCurrent(identity, origin)
                tokens.write(record)
                state.token = record
                state.failures = 0
                state.cooldownUntil = 0
                state.unsupported = false
            }
            return record
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val (unsupported, refusal) = mutex.withLock {
                state.unsupported = failure.isAttestationUnsupported()
                if (failure is HttpException) {
                    val code = failure.peekErrorCode()
                    state.refusal = bootstrapRefusal(failure.code(), code)?.let { Refusal(failure.code(), code) }
                    state.cooldownUntil = if (failure.code() == 429) {
                        retryDeadline(failure.response()?.headers()?.get("Retry-After"))
                    } else {
                        backoffDeadline(
                            state
                        )
                    }
                } else {
                    state.cooldownUntil = backoffDeadline(state)
                }
                state.unsupported to state.refusal
            }
            if (unsupported) throw AttestationUnsupportedException("Profile-3 bootstrap failed", failure)
            if (refusal != null) throw AttestationRefusedException(refusal.status, refusal.code, "Profile-3 bootstrap was refused", failure)
            throw AttestationUnavailableException("Profile-3 bootstrap failed", failure)
        } finally {
            val job = currentCoroutineContext()[Job]
            withContext(NonCancellable) {
                mutex.withLock {
                    if (state.issuance === job) state.issuance = null
                    if (identities.current()?.scope != identity.scope) {
                        keys.delete(identity.keyAlias)
                        tokens.remove(identity.scope)
                    }
                }
            }
        }
    }

    private suspend fun register(identity: Profile3Identity, origin: HttpUrl) {
        bounded(100_000L) { registration.register(identity, origin) }
        mutex.withLock {
            requireCurrent(identity, origin)
            identities.markRegistered(identity.scope)
        }
    }

    private suspend fun issueWithRecovery(identity: Profile3Identity, origin: HttpUrl): Profile3TokenRecord {
        return try {
            issue(identity, origin)
        } catch (failure: HttpException) {
            when (failure.peekErrorCode()) {
                "unknown_client" -> {
                    if (failure.code() != 401) throw failure
                    register(identity, origin)
                }
                "invalid_challenge" -> if (failure.code() != 401) throw failure
                else -> throw failure
            }
            // Exactly one fresh challenge/proof; a sent proof is never replayed.
            issue(identity, origin)
        }
    }

    private suspend fun issue(identity: Profile3Identity, origin: HttpUrl): Profile3TokenRecord = bounded(20_000L) {
        requireConfiguredOrigin(urls, origin)
        val challenge = requireProfile3Challenge(
            api.challenge(endpoint(origin, "token-challenges"), Profile3ChallengeRequest(identity.scope.clientId, "token"))
        )
        requireConfiguredOrigin(urls, origin)
        val url = endpoint(origin, "tokens")
        val context = signedContext("POST", requireNotNull(origin.resolve(url)), "application/json")
        val binding = AttestationSigning.sha256(Profile3Signing.issuancePreimage(identity.scope.origin))
        val digest = Profile3Signing.requestDigest(Profile3Signing.Purpose.TOKEN, challenge, identity.scope.clientId, context, binding)
        val signature = AttestationSigning.base64(keys.sign(identity.keyAlias, digest))
        val request = Profile3TokenRequest(identity.scope.clientId, challenge, signature)
        val start = clock.elapsedRealtimeMillis()
        val boot = clock.bootCount()
        val response = api.token(url, request)
        tokenFactory.create(identity.scope, response.accessToken, response.issuedAt, response.expiresAt, start, boot)
    }

    private suspend fun requireCurrent(identity: Profile3Identity, origin: HttpUrl) {
        requireConfiguredOrigin(urls, origin)
        if (identities.current()?.scope != identity.scope) throw IOException("Profile-3 identity changed")
        currentCoroutineContext().ensureActive()
    }

    private suspend fun <T> bounded(timeoutMillis: Long, work: suspend () -> T): T = try {
        withTimeout(timeoutMillis) { work() }
    } catch (timeout: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        throw IOException("Profile-3 bootstrap timed out", timeout)
    }

    private fun retryDeadline(retryAfter: String?): Long {
        val seconds = retryAfter?.toLongOrNull()?.takeIf { it >= 0 } ?: 30L
        val now = clock.elapsedRealtimeMillis()
        return now + seconds.coerceAtMost((Long.MAX_VALUE - now) / 1_000) * 1_000
    }

    private fun backoffDeadline(state: OriginState): Long {
        val seconds = minOf(30L, 1L shl state.failures.coerceAtMost(5))
        state.failures += 1
        return clock.elapsedRealtimeMillis() + seconds * 1_000 + (random.nextDouble() * 1_000).toLong()
    }
}
