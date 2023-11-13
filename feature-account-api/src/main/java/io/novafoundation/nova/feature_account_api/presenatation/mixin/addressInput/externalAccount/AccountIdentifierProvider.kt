package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException
import kotlinx.coroutines.flow.Flow
import io.novafoundation.nova.common.utils.Event as OneShotEvent

interface AccountIdentifierProvider {

    val selectedExternalAccountFlow: Flow<ExtendedLoadingState<ExternalAccount?>>

    val eventsLiveData: LiveData<OneShotEvent<Event>>

    fun selectExternalAccount(account: ExternalAccount?)

    fun isIdentifierValid(raw: String): Boolean

    fun loadExternalAccounts(raw: String)

    sealed interface Event {

        class ShowBottomSheetEvent(
            val identifier: String,
            val chainName: String,
            val externalAccounts: List<ExternalAccount>,
            val selectedAccount: ExternalAccount?
        ) : Event

        class ErrorEvent(val exception: Web3NamesException) : Event
    }

    fun interface Factory {

        fun create(input: Flow<String>): AccountIdentifierProvider
    }
}
