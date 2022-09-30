package io.novafoundation.nova.feature_vote.di

import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase

interface VoteFeatureDependencies {

    val selectedAccountUseCase: SelectedAccountUseCase
}
