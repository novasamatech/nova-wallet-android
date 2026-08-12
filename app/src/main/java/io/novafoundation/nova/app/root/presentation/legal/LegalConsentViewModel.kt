package io.novafoundation.nova.app.root.presentation.legal

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.legal.LegalConsentRepository
import io.novafoundation.nova.common.data.legal.LegalDocument
import io.novafoundation.nova.common.data.legal.LegalDocumentType
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale

class LegalConsentModel(
    val type: LegalDocumentType,
    val updatedAt: String
)

class LegalConsentViewModel(
    private val legalConsentRepository: LegalConsentRepository,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel(),
    Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val closeEvent = MutableLiveData<Event<Unit>>()

    private val documents = flowOf { legalConsentRepository.getDocuments().orEmpty() }
        .shareInBackground()

    private val acceptedTypes = MutableStateFlow(emptySet<LegalDocumentType>())

    val documentModels: Flow<List<LegalConsentModel>> = documents.map { documents ->
        documents.map { LegalConsentModel(type = it.type, updatedAt = formatUpdatedAt(it)) }
    }

    val canProceed: Flow<Boolean> = combine(documents, acceptedTypes) { documents, accepted ->
        documents.isNotEmpty() && accepted.containsAll(documents.map(LegalDocument::type))
    }

    fun documentCheckChanged(type: LegalDocumentType, checked: Boolean) {
        acceptedTypes.value = if (checked) acceptedTypes.value + type else acceptedTypes.value - type
    }

    fun documentClicked(type: LegalDocumentType) {
        openBrowserEvent.value = Event(urlOf(type))
    }

    fun acceptClicked() {
        legalConsentRepository.acceptCurrentVersions()

        closeEvent.value = Unit.event()
    }

    private fun urlOf(type: LegalDocumentType) = when (type) {
        LegalDocumentType.TERMS_OF_SERVICE -> appLinksProvider.termsUrl
        LegalDocumentType.PRIVACY_NOTICE -> appLinksProvider.privacyUrl
    }

    private fun formatUpdatedAt(document: LegalDocument): String {
        return SimpleDateFormat(UPDATED_AT_DISPLAY_FORMAT, Locale.getDefault()).format(document.updatedAt)
    }

    private companion object {

        const val UPDATED_AT_DISPLAY_FORMAT = "d MMMM yyyy"
    }
}
