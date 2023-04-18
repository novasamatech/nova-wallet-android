package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.providers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.loadedNothing
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.AccountIdentifierProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.ExternalAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class EmptyAccountIdentifierProvider : AccountIdentifierProvider {

    override val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>> = flowOf(loadedNothing())

    override val eventsLiveData: LiveData<Event<AccountIdentifierProvider.Event>> = MutableLiveData()

    override fun selectExternalAccount(account: ExternalAccount?) {
        // empty implementation
    }

    override fun loadExternalAccounts(raw: String) {
        // empty implementation
    }

    override fun isIdentifierValid(raw: String) = false
}
