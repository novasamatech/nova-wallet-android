package io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.feature_ahm_impl.R
import io.novafoundation.nova.feature_ahm_impl.domain.ChainMigrationDetailsInteractor
import io.novafoundation.nova.feature_ahm_impl.presentation.ChainMigrationRouter
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.forDirectory
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfigModel(
    val title: String,
    val minimalBalance: String,
    val lowerFee: String,
    val tokens: String,
    val unifiedAccess: String,
    val anyTokenFee: String
)

class ChainMigrationDetailsViewModel(
    private val resourceManager: ResourceManager,
    private val router: ChainMigrationRouter,
    private val interactor: ChainMigrationDetailsInteractor,
    private val payload: ChainMigrationDetailsPayload,
    private val promotionBannersMixinFactory: PromotionBannersMixinFactory,
    private val bannerSourceFactory: BannersSourceFactory,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val dateFormatter = SimpleDateFormat("d MMMM, yyyy", Locale.getDefault())

    private val chainFlow = interactor.chainFlow(payload.chainId)
        .shareInBackground()

    private val configFlow = flowOf { interactor.getChainMigrationConfig(payload.chainId) }
        .shareInBackground()

    val bannersFlow = configFlow.map {
        promotionBannersMixinFactory.create(bannerSourceFactory.forDirectory(it.bannerPath), this)
    }.shareInBackground()

    val configUIFlow = combine(configFlow, chainFlow) { config, sourceChain ->
        val destinationChain = interactor.getChain(config.destinationData.chainId)
        val sourceAsset = sourceChain.assetsById.getValue(config.sourceData.assetId)
        val destinationAsset = destinationChain.assetsById.getValue(config.destinationData.assetId)
        val tokenSymbol = sourceAsset.symbol.value
        val newTokens = config.newTokenNames.joinToString()

        val formattedDate = dateFormatter.format(config.timeStartAt)
        val minimalBalanceScale = config.sourceData.minBalance / config.destinationData.minBalance
        val lowerFeeScale = config.sourceData.averageFee / config.destinationData.averageFee

        ConfigModel(
            title = resourceManager.getString(R.string.chain_migration_details_title, formattedDate, tokenSymbol, destinationChain.name),
            minimalBalance = resourceManager.getString(
                R.string.chain_migration_details_minimal_balance,
                minimalBalanceScale.format(),
                config.sourceData.minBalance.amountFromPlanks(sourceAsset.precision).format(),
                config.destinationData.minBalance.amountFromPlanks(destinationAsset.precision).format(),
            ),
            lowerFee = resourceManager.getString(
                R.string.chain_migration_details_lower_fee,
                lowerFeeScale.format(),
                config.sourceData.averageFee.amountFromPlanks(sourceAsset.precision).format(),
                config.destinationData.averageFee.amountFromPlanks(destinationAsset.precision).format(),
            ),
            tokens = resourceManager.getString(R.string.chain_migration_details_tokens, newTokens),
            unifiedAccess = resourceManager.getString(R.string.chain_migration_details_unified_access, tokenSymbol),
            anyTokenFee = resourceManager.getString(R.string.chain_migration_details_fee_in_any_tokens),
        )
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
