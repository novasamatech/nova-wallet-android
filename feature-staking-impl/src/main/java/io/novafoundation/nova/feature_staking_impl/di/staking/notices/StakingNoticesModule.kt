package io.novafoundation.nova.feature_staking_impl.di.staking.notices

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.feature_staking_impl.data.notices.cache.StakingNoticesDiskCache
import io.novafoundation.nova.feature_staking_impl.data.notices.network.StakingNoticesApi
import io.novafoundation.nova.feature_staking_impl.data.notices.repository.RealStakingNoticesRepository
import io.novafoundation.nova.feature_staking_impl.data.notices.repository.StakingNoticesRepository

@Module
class StakingNoticesModule {

    @Provides
    @FeatureScope
    fun provideStakingNoticesApi(networkApiCreator: NetworkApiCreator): StakingNoticesApi =
        networkApiCreator.create(StakingNoticesApi::class.java)

    @Provides
    @FeatureScope
    fun provideStakingNoticesDiskCache(context: Context): StakingNoticesDiskCache =
        StakingNoticesDiskCache(context)

    @Provides
    @FeatureScope
    fun provideStakingNoticesRepository(
        api: StakingNoticesApi,
        diskCache: StakingNoticesDiskCache,
        gson: Gson,
        sharedPreferences: SharedPreferences,
    ): StakingNoticesRepository = RealStakingNoticesRepository(
        api = api,
        diskCache = diskCache,
        gson = gson,
        sharedPreferences = sharedPreferences,
        sharedPreferencesLanguageKey = LANGUAGE_KEY,
        remoteUrl = BuildConfig.STAKING_NOTICES_URL,
    )

    private companion object {
        const val LANGUAGE_KEY = "selected_language"
    }
}
