package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.main

import android.os.Parcelable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.CrossChainRewardDestination
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.ConfirmContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.SelectContributeCustomization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class MoonbeamRewardDestinationUi(
    val addressModel: AddressModel,
    val chain: ChainUi,
    val title: String,
)

class MoonbeamMainFlowCustomViewStateFactory(
    private val interactor: MoonbeamCrowdloanInteractor,
    private val resourceManager: ResourceManager,
    private val iconGenerator: AddressIconGenerator,
) {

    fun create(scope: CoroutineScope, parachainMetadata: ParachainMetadata): MoonbeamMainFlowCustomViewState {
        return MoonbeamMainFlowCustomViewState(scope, parachainMetadata, interactor, resourceManager, iconGenerator)
    }
}

class MoonbeamMainFlowCustomViewState(
    coroutineScope: CoroutineScope,
    private val parachainMetadata: ParachainMetadata,
    interactor: MoonbeamCrowdloanInteractor,
    private val resourceManager: ResourceManager,
    private val iconGenerator: AddressIconGenerator,
) :
    SelectContributeCustomization.ViewState,
    ConfirmContributeCustomization.ViewState,
    CoroutineScope by coroutineScope {

    val moonbeamRewardDestination = flowOf { interactor.getMoonbeamRewardDestination(parachainMetadata) }
        .map(::mapMoonbeamChainDestinationToUi)
        .inBackground()
        .shareIn(this, started = SharingStarted.Eagerly, replay = 1)

    private suspend fun mapMoonbeamChainDestinationToUi(crossChainRewardDestination: CrossChainRewardDestination): MoonbeamRewardDestinationUi {
        return MoonbeamRewardDestinationUi(
            addressModel = iconGenerator.createAddressModel(
                chain = crossChainRewardDestination.destination,
                address = crossChainRewardDestination.addressInDestination,
                sizeInDp = AddressIconGenerator.SIZE_SMALL,
                accountName = null
            ),
            chain = mapChainToUi(crossChainRewardDestination.destination),
            title = resourceManager.getString(R.string.crowdloan_moonbeam_reward_destination, parachainMetadata.token)
        )
    }

    override val customizationPayloadFlow: Flow<Parcelable?> = kotlinx.coroutines.flow.flowOf(null)
}
