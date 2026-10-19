package io.novafoundation.nova.infrastructure.di

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.profile3.Profile3AccessTokenProvider

interface InfrastructureApi {

    val profile3AccessTokenProvider: Profile3AccessTokenProvider

    val infrastructureUrls: InfrastructureUrls

    @get:Attested
    val attestedNetworkApiCreator: NetworkApiCreator
}
