package io.novafoundation.nova.infrastructure.di

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.ClientAttestationService

interface InfrastructureApi {

    val clientAttestationService: ClientAttestationService

    val infrastructureUrls: InfrastructureUrls

    @get:Attested
    val attestedNetworkApiCreator: NetworkApiCreator
}
