package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_governance_api.data.repository.TreasuryRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.data.preimage.PreImageSizer
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.RealReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.RealReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallAdapter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.batch.BatchAdapter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury.TreasuryApproveProposalAdapter
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury.TreasurySpendAdapter
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ReferendumDetailsModule {

    @Provides
    @FeatureScope
    @IntoSet
    fun provideTreasuryApproveParser(
        treasuryRepository: TreasuryRepository
    ): ReferendumCallAdapter = TreasuryApproveProposalAdapter(treasuryRepository)

    @Provides
    @FeatureScope
    @IntoSet
    fun provideTreasurySpendParser(): ReferendumCallAdapter = TreasurySpendAdapter()

    @Provides
    @FeatureScope
    @IntoSet
    fun provideBatchAdapter(): ReferendumCallAdapter = BatchAdapter()

    @Provides
    @FeatureScope
    fun providePreImageParser(
        callAdapters: Set<@JvmSuppressWildcards ReferendumCallAdapter>
    ): ReferendumPreImageParser {
        return RealReferendumPreImageParser(callAdapters)
    }

    @Provides
    @FeatureScope
    fun provideReferendumDetailsInteractor(
        preImageParser: ReferendumPreImageParser,
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        referendaConstructor: ReferendaConstructor,
        preImageSizer: PreImageSizer,
        @ExtrinsicSerialization callFormatter: Gson,
        identityRepository: OnChainIdentityRepository,
    ): ReferendumDetailsInteractor = RealReferendumDetailsInteractor(
        preImageParser = preImageParser,
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        referendaConstructor = referendaConstructor,
        preImageSizer = preImageSizer,
        callFormatter = callFormatter,
        identityRepository = identityRepository,
    )
}
