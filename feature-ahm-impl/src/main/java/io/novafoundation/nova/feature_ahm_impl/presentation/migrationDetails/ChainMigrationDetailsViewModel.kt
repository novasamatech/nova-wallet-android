package io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.mixin.hints.HintModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.feature_ahm_api.presentation.getChainMigrationDateFormat
import io.novafoundation.nova.feature_ahm_impl.R
import io.novafoundation.nova.feature_ahm_impl.domain.ChainMigrationDetailsInteractor
import io.novafoundation.nova.feature_ahm_impl.presentation.ChainMigrationRouter
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.forDirectory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChainMigrationDetailsViewModel(
    private val resourceManager: ResourceManager,
    private val router: ChainMigrationRouter,
    private val interactor: ChainMigrationDetailsInteractor,
    private val payload: ChainMigrationDetailsPayload,
    private val promotionBannersMixinFactory: PromotionBannersMixinFactory,
    private val bannerSourceFactory: BannersSourceFactory,
    private val tokenFormatter: TokenFormatter
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val dateFormatter = getChainMigrationDateFormat()

    private val chainFlow = interactor.chainFlow(payload.chainId)
        .shareInBackground()

    private val configFlow = flowOf { interactor.getChainMigrationConfig(payload.chainId) }
        .filterNotNull()
        .shareInBackground()

    val bannersFlow = configFlow.map {
        promotionBannersMixinFactory.create(bannerSourceFactory.forDirectory(it.bannerPath), this)
    }.shareInBackground()

    val configUIFlow = combine(configFlow, chainFlow) { config, sourceChain ->
        val destinationChain = interactor.getChain(config.destinationData.chainId)
        val sourceAsset = sourceChain.assetsById.getValue(config.originData.assetId)
        val destinationAsset = destinationChain.assetsById.getValue(config.destinationData.assetId)
        val tokenSymbol = sourceAsset.symbol.value
        val newTokens = config.newTokenNames.joinToString()

        val minimalBalanceScale = config.originData.minBalance / config.destinationData.minBalance
        val lowerFeeScale = config.originData.averageFee / config.destinationData.averageFee

        ConfigModel(
            title = getTitle(config, tokenSymbol, destinationChain),
            minimalBalance = resourceManager.getString(
                R.string.chain_migration_details_minimal_balance,
                minimalBalanceScale.format(),
                tokenFormatter.formatToken(config.originData.minBalance, sourceAsset),
                tokenFormatter.formatToken(config.destinationData.minBalance, destinationAsset),
            ),
            lowerFee = resourceManager.getString(
                R.string.chain_migration_details_lower_fee,
                lowerFeeScale.format(),
                tokenFormatter.formatToken(config.originData.averageFee, sourceAsset),
                tokenFormatter.formatToken(config.destinationData.averageFee, destinationAsset)
            ),
            tokens = resourceManager.getString(R.string.chain_migration_details_tokens, newTokens),
            unifiedAccess = resourceManager.getString(R.string.chain_migration_details_unified_access, tokenSymbol),
            anyTokenFee = resourceManager.getString(R.string.chain_migration_details_fee_in_any_tokens),
            hints = listOf(
                HintModel(R.drawable.ic_recent_history, resourceManager.getString(R.string.chain_migration_details_hint_history, sourceChain.name)),
                HintModel(R.drawable.ic_nova, resourceManager.getString(R.string.chain_migration_details_hint_auto_migration))
            )
        )
    }

    private fun getTitle(config: ChainMigrationConfig, tokenSymbol: String, destinationChain: Chain): String {
        val formattedDate = dateFormatter.format(config.timeStartAt)
        return if (config.migrationInProgress) {
            resourceManager.getString(R.string.chain_migration_details_in_progress_title, formattedDate, tokenSymbol, destinationChain.name)
        } else {
            resourceManager.getString(R.string.chain_migration_details_title, formattedDate, tokenSymbol, destinationChain.name)
        }
    }

    fun okButtonClicked() = launchUnit {
        interactor.markMigrationInfoAlreadyShown(payload.chainId)
        router.back()
    }

    fun learnMoreClicked() {
        launch {
            val config = configFlow.first()

            openBrowserEvent.value = Event(config.wikiURL)
        }
    }
}
