package io.novafoundation.nova.feature_ledger_core

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_core.domain.RealLedgerMigrationTracker
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class LedgerFeatureModule {

   @Provides
   @FeatureScope
   fun provideLedgerMigrationTracker(
       metadataShortenerService: MetadataShortenerService,
       chainRegistry: ChainRegistry
   ): LedgerMigrationTracker {
       return RealLedgerMigrationTracker(metadataShortenerService, chainRegistry)
   }
}
