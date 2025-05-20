package io.novafoundation.nova.feature_ledger_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.di.annotations.GenericLedger
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory

@Module
class GenericLedgerModule {

    @Provides
    @FeatureScope
    @GenericLedger
    fun provideMessageFormatter(factory: LedgerMessageFormatterFactory): LedgerMessageFormatter = factory.createGeneric()


    @Provides
    @FeatureScope
    @GenericLedger
    fun provideMessageCommandFormatter(
        @GenericLedger  messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)
}
