package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom

interface PrivateCrowdloanSignatureProvider {

    // TODO method signature tbd
    suspend fun provideSignature(): String
}
