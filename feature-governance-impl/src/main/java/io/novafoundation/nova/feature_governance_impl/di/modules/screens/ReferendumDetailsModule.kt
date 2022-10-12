package io.novafoundation.nova.feature_governance_impl.di.modules.screens

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_governance_api.data.repository.TreasuryRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.RealReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury.TreasuryApproveProposalParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury.TreasurySpendParser
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository

@Module
class ReferendumDetailsModule {

    @Provides
    @FeatureScope
    @IntoSet
    fun provideTreasuryApproveParser(
        treasuryRepository: TreasuryRepository
    ): ReferendumCallParser = TreasuryApproveProposalParser(treasuryRepository)

    @Provides
    @FeatureScope
    @IntoSet
    fun provideTreasurySpendParser(): ReferendumCallParser = TreasurySpendParser()

    @Provides
    @FeatureScope
    fun provideReferendumDetailsInteractor(
        callParsers: Set<@JvmSuppressWildcards ReferendumCallParser>,
        preImageRepository: PreImageRepository,
        governanceSourceRegistry: GovernanceSourceRegistry,
        chainStateRepository: ChainStateRepository,
        totalIssuanceRepository: TotalIssuanceRepository,
        referendaConstructor: ReferendaConstructor,
    ): ReferendumDetailsInteractor = RealReferendumDetailsInteractor(
        preImageParsers = callParsers,
        preImageRepository = preImageRepository,
        governanceSourceRegistry = governanceSourceRegistry,
        chainStateRepository = chainStateRepository,
        totalIssuanceRepository = totalIssuanceRepository,
        referendaConstructor = referendaConstructor
    )
}
