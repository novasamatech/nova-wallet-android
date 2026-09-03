package io.novafoundation.nova.infrastructure.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.IntegrityService
import io.novafoundation.nova.infrastructure.BuildConfig
import io.novafoundation.nova.infrastructure.attestation.AttestationApi
import io.novafoundation.nova.infrastructure.attestation.AttestationIdentity
import io.novafoundation.nova.infrastructure.attestation.AttestationInterceptor
import io.novafoundation.nova.infrastructure.attestation.AttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.AttestationMode
import io.novafoundation.nova.infrastructure.attestation.ClientAttestationService
import io.novafoundation.nova.infrastructure.attestation.RealAttestationIdentity
import io.novafoundation.nova.infrastructure.attestation.RealAttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.RealClientAttestationService
import javax.inject.Qualifier
import okhttp3.OkHttpClient

/** Marks a [NetworkApiCreator] whose requests are signed with attestation headers. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Attested

@Module
class AttestationModule {

    @Provides
    @ApplicationScope
    fun provideAttestationIdentity(preferences: Preferences): AttestationIdentity {
        return RealAttestationIdentity(preferences)
    }

    @Provides
    @ApplicationScope
    fun provideAttestationKeyPairStore(): AttestationKeyPairStore = RealAttestationKeyPairStore()

    @Provides
    @ApplicationScope
    fun provideAttestationApi(networkApiCreator: NetworkApiCreator): AttestationApi {
        return networkApiCreator.create(AttestationApi::class.java, BuildConfig.INFRASTRUCTURE_HOST)
    }

    @Provides
    @ApplicationScope
    fun provideClientAttestationService(
        context: Context,
        api: AttestationApi,
        identity: AttestationIdentity,
        keyPairStore: AttestationKeyPairStore,
        integrityService: IntegrityService
    ): ClientAttestationService {
        // The build type decided this, not a runtime guess: see infrastructure/build.gradle.
        val mode = AttestationMode.fromWireName(BuildConfig.ATTESTATION_MODE)
        val sharedSecret = BuildConfig.ATTESTATION_SHARED_SECRET.takeIf { it.isNotBlank() }

        require(mode != AttestationMode.SHARED_SECRET || sharedSecret != null) {
            "shared_secret attestation needs DEBUG_ATTESTATION_SHARED_SECRET in local.properties"
        }

        return RealClientAttestationService(api, identity, keyPairStore, integrityService, context.packageName, mode, sharedSecret)
    }

    @Provides
    @ApplicationScope
    @Attested
    fun provideAttestedNetworkApiCreator(
        okHttpClient: OkHttpClient,
        attestationService: ClientAttestationService
    ): NetworkApiCreator {
        val attestedClient = okHttpClient.newBuilder()
            .addInterceptor(AttestationInterceptor(attestationService))
            .build()

        return NetworkApiCreator(attestedClient, "https://placeholder.com")
    }
}
