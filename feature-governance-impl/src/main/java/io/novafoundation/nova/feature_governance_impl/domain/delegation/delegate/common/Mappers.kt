package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.DelegateAccountType


fun mapAccountTypeToDomain(isOrganization: Boolean): DelegateAccountType {
    return if (isOrganization) DelegateAccountType.ORGANIZATION else DelegateAccountType.INDIVIDUAL
}
