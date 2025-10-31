package io.novafoundation.nova.feature_gift_impl.di

import android.content.Context
import coil.ImageLoader
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.IntegrityService
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface GiftFeatureDependencies {

    val amountFormatter: AmountFormatter

    val context: Context

    val preferences: Preferences

    val integrityService: IntegrityService

    fun giftsDao(): GiftsDao

    fun resourceManager(): ResourceManager

    fun appLinksProvider(): AppLinksProvider

    fun chainRegistry(): ChainRegistry

    fun imageLoader(): ImageLoader

    fun secretStoreV2(): SecretStoreV2

    fun apiCreator(): NetworkApiCreator
}
