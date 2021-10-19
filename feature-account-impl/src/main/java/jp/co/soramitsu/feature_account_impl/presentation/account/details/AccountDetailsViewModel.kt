package jp.co.soramitsu.feature_account_impl.presentation.account.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.list.headers.TextHeader
import jp.co.soramitsu.common.list.toListWithHeaders
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.flowOf
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import jp.co.soramitsu.feature_account_api.data.mappers.mapChainToUi
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_account_impl.R
import jp.co.soramitsu.feature_account_impl.domain.account.details.AccountDetailsInteractor
import jp.co.soramitsu.feature_account_impl.domain.account.details.AccountInChain
import jp.co.soramitsu.feature_account_impl.domain.account.details.AvailableExportType
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportPayload
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportSource
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private const val UPDATE_NAME_INTERVAL_SECONDS = 1L

class ExportChooserPayload(
    val chain: Chain,
    val dynamicListPayload: DynamicListBottomSheet.Payload<ExportSource>
)

class AccountDetailsViewModel(
    private val interactor: AccountDetailsInteractor,
    private val accountRouter: AccountRouter,
    private val iconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val metaId: Long,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
) : BaseViewModel(), ExternalActions by externalActions {

    val accountNameFlow: MutableStateFlow<String> = MutableStateFlow("")

    private val metaAccount = async(Dispatchers.Default) { interactor.getMetaAccount(metaId) }

    private val _showExportSourceChooser = MutableLiveData<Event<ExportChooserPayload>>()
    val showExportSourceChooser: LiveData<Event<ExportChooserPayload>> = _showExportSourceChooser

    val chainAccountProjections = flowOf { interactor.getChainProjections(metaAccount()) }
        .map { groupedList ->
            groupedList.mapKeys { (from, _) -> mapFromToTextHeader(from) }
                .mapValues { (_, accounts) -> accounts.map { mapChainAccountProjectionToUi(it) } }
                .toListWithHeaders()
        }
        .inBackground()
        .share()

    init {
        launch {
            accountNameFlow.emit(metaAccount().name)
        }

        syncNameChangesWithDb()
    }

    fun backClicked() {
        accountRouter.back()
    }

    @OptIn(ExperimentalTime::class)
    private fun syncNameChangesWithDb() {
        accountNameFlow
            .filter { it.isNotEmpty() }
            .debounce(UPDATE_NAME_INTERVAL_SECONDS.seconds)
            .onEach { interactor.updateName(metaId, it) }
            .launchIn(viewModelScope)
    }

    private fun mapFromToTextHeader(from: AccountInChain.From): TextHeader {
        val resId = when (from) {
            AccountInChain.From.META_ACCOUNT -> R.string.account_shared_secret
            AccountInChain.From.CHAIN_ACCOUNT -> R.string.account_custom_secret
        }

        return TextHeader(resourceManager.getString(resId))
    }

    private suspend fun mapChainAccountProjectionToUi(accountInChain: AccountInChain) = with(accountInChain) {
        val addressOrHint = projection?.address ?: resourceManager.getString(R.string.account_no_chain_projection)
        val accountIcon = projection?.let {
            iconGenerator.createAddressIcon(it.accountId, AddressIconGenerator.SIZE_SMALL, backgroundColorRes = R.color.account_icon_dark)
        } ?: resourceManager.getDrawable(R.drawable.ic_warning_filled)

        AccountInChainUi(
            chainUi = mapChainToUi(chain),
            addressOrHint = addressOrHint,
            address = projection?.address,
            accountIcon = accountIcon
        )
    }

    fun chainAccountClicked(item: AccountInChainUi) = launch {
        val chain = chainRegistry.getChain(item.chainUi.id)

        val type = if (item.address != null) {
            ExternalActions.Type.Address(item.address)
        } else {
            ExternalActions.Type.None(item.addressOrHint)
        }

        externalActions.showExternalActions(type, chain)
    }

    fun exportClicked(inChain: Chain) = launch {
        viewModelScope.launch {
            val sources = interactor.availableExportTypes(metaAccount(), inChain)
                .map(::mapAvailableExportTypeToUI)

            _showExportSourceChooser.value = Event(
                ExportChooserPayload(
                    chain = inChain,
                    dynamicListPayload = DynamicListBottomSheet.Payload(sources)
                )
            )
        }
    }

    fun exportTypeChosen(exportSource: ExportSource, chain: Chain) {
        val exportPayload = ExportPayload(metaId, chain.id)

        val navigationAction = when(exportSource) {
            ExportSource.Mnemonic -> accountRouter.exportMnemonicAction(exportPayload)
            ExportSource.Json -> accountRouter.exportJsonPasswordAction(exportPayload)
            ExportSource.Seed -> accountRouter.exportSeedAction(exportPayload)
        }

        accountRouter.withPinCodeCheckRequired(navigationAction)
    }

    fun changeChainAccountClicked(inChain: Chain) {
        accountRouter.openAddAccount(AddAccountPayload.ChainAccount(inChain.id, metaId))
    }

    private fun mapAvailableExportTypeToUI(
        availableExportType: AvailableExportType
    ): ExportSource {
        return when (availableExportType) {
            AvailableExportType.MNEMONC -> ExportSource.Mnemonic
            AvailableExportType.SEED -> ExportSource.Seed
            AvailableExportType.JSON -> ExportSource.Json
        }
    }
}
