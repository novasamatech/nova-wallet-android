package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import kotlinx.coroutines.flow.Flow

interface ExternalAccountResolver {

    val externalIdentifierEventLiveData: LiveData<AccountIdentifierProvider.Event>

    val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>>

    fun selectedExternalAddressClicked()

    fun loadExternalIdentifiers()

    fun selectExternalAccount(externalAccount: ExternalAccount)
}
