package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.DefaultMutableSharedState
import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_impl.data.repository.ParitySignerRepository
import io.novafoundation.nova.feature_account_impl.data.repository.RealParitySignerRepository
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.QrCodeExpiredPresentableFactory
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

@Module
class ParitySignerModule {

    @Provides
    @FeatureScope
    fun provideSignSharedState(): MutableSharedState<SignerPayloadExtrinsic> = DefaultMutableSharedState()

    @Provides
    @FeatureScope
    fun provideReadOnlySharedState(
        mutableSharedState: MutableSharedState<SignerPayloadExtrinsic>
    ): SharedState<SignerPayloadExtrinsic> = mutableSharedState

    @Provides
    @FeatureScope
    fun provideRepository(
        accountDao: MetaAccountDao
    ): ParitySignerRepository = RealParitySignerRepository(accountDao)

    @Provides
    @FeatureScope
    fun provideQrCodeExpiredPresentableFactory(
        resourceManager: ResourceManager,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        router: AccountRouter,
    ) = QrCodeExpiredPresentableFactory(resourceManager, actionAwaitableMixinFactory, router)
}
