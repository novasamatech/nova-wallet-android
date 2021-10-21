package io.novafoundation.nova.feature_account_impl.presentation.exporting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.cryptoTypeIn
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.map

abstract class ExportViewModel(
    protected val accountInteractor: AccountInteractor,
    protected val exportPayload: ExportPayload,
    protected val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    val exportSource: ExportSource
) : BaseViewModel() {

    protected val chain by lazyAsync {
        chainRegistry.getChain(exportPayload.chainId)
    }

    private val _exportEvent = MutableLiveData<Event<String>>()
    val exportEvent: LiveData<Event<String>> = _exportEvent

    private val metaAccountLiveData = flowOf {
        accountInteractor.getMetaAccount(exportPayload.metaId)
    }

    val cryptoTypeFlow = metaAccountLiveData.map {
        val cryptoType = it.cryptoTypeIn(chain())

        mapCryptoTypeToCryptoTypeModel(resourceManager, cryptoType)
    }
        .inBackground()
        .share()

    val chainUiFlow = flowOf { mapChainToUi(chain()) }
        .share()
        .inBackground()

    private val _showSecurityWarningEvent = MutableLiveData<Event<Unit>>()
    val showSecurityWarningEvent = _showSecurityWarningEvent

    protected fun showSecurityWarning() {
        _showSecurityWarningEvent.sendEvent()
    }

    protected fun exportText(text: String) {
        _exportEvent.value = Event(text)
    }

    open fun securityWarningConfirmed() {
        // optional override
    }
}
