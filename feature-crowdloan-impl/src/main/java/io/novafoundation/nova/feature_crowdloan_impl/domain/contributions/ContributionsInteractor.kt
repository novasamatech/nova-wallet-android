package io.novafoundation.nova.feature_crowdloan_impl.domain.contributions

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ContributionSource
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import java.math.BigInteger

class ContributionsInteractor(
    private val source: ContributionSource,
    private val crowdloanRepository: CrowdloanRepository,
    private val accountRepository: AccountRepository,
    private val selectedAssetState: SingleAssetSharedState,
) {

    suspend fun getUserContributions(): List<Contribution> {
        val chain = selectedAssetState.chain()
        val metaAccount = accountRepository.getSelectedMetaAccount()

        if (crowdloanRepository.isCrowdloansAvailable(chain.id).not()) {
            return emptyList()
        }

        val parachainMetadatas = runCatching {
            crowdloanRepository.getParachainMetadata(chain)
        }.getOrDefault(emptyMap())

        val fundInfos = crowdloanRepository.allFundInfos(chain.id)

        return source.getContributions(
            chain = chain,
            accountId = metaAccount.accountIdIn(chain)!!,
            funds = fundInfos
        )
            .filter { it.amount > BigInteger.ZERO }
            .map {
                Contribution(
                    amount = it.amount,
                    parachainMetadata = parachainMetadatas[it.paraId],
                    sourceName = it.sourceName,
                    fundInfo = fundInfos.getValue(it.paraId),
                    paraId = it.paraId
                )
            }
    }
}
