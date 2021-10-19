package jp.co.soramitsu.feature_account_impl.presentation.exporting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.flowOf
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.common.utils.sendEvent
import jp.co.soramitsu.feature_account_api.data.mappers.mapChainToUi
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_api.domain.model.cryptoTypeIn
import jp.co.soramitsu.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
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
        val cryptoType =  it.cryptoTypeIn(chain())

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
