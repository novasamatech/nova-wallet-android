package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.Event
import kotlinx.coroutines.flow.Flow

interface ExternalAccountResolver {

    val externalIdentifierEventLiveData: LiveData<Event<AccountIdentifierProvider.Event>>

    val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>>

    fun selectedExternalAddressClicked()

    fun loadExternalIdentifiers()

    fun selectExternalAccount(externalAccount: ExternalAccount)
}
