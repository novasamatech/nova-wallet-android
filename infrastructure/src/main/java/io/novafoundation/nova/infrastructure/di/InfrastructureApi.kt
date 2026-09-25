package io.novafoundation.nova.infrastructure.di

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.infrastructure.attestation.ClientAttestationService

interface InfrastructureApi {

    val infrastructureBaseUrl: String

    val clientAttestationService: ClientAttestationService

    @get:Attested
    val attestedNetworkApiCreator: NetworkApiCreator
}
