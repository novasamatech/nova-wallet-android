package io.novafoundation.nova.feature_staking_impl.di.staking

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class PayoutsBulkRetriever

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class DefaultBulkRetriever
