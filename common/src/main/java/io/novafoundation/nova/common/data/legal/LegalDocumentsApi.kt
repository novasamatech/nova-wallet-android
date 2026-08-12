package io.novafoundation.nova.common.data.legal

import io.novafoundation.nova.common.BuildConfig
import retrofit2.http.GET

interface LegalDocumentsApi {

    @GET(BuildConfig.LEGAL_DOCUMENTS_URL)
    suspend fun getLegalDocuments(): LegalDocumentsRemote
}
