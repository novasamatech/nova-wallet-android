package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam

import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.PrivateCrowdloanSignatureProvider

class MoonbeamPrivateSignatureProvider(
    val interactor: MoonbeamCrowdloanInteractor,
) : PrivateCrowdloanSignatureProvider {

    override suspend fun provideSignature(): String {
        TODO("Not yet implemented")
    }
}
