package io.novafoundation.nova.feature_dapp_impl.data.network.phishing

import retrofit2.http.GET

interface PhishingSitesApi {

    @GET("https://raw.githubusercontent.com/polkadot-js/phishing/master/all.json")
    suspend fun getPhishingSites(): PhishingSitesRemote
}
