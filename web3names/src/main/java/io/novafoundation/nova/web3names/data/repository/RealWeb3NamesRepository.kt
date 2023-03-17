package io.novafoundation.nova.web3names.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.data.caip19.Caip19MatcherFactory
import io.novafoundation.nova.web3names.data.caip19.Caip19Parser
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.provider.Web3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.domain.models.Web3NameAccount
import io.novafoundation.nova.web3names.domain.repository.Web3NamesRepository
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

private const val TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE = "KiltTransferAssetRecipientV1"

class ServiceEndpoint(
    val serviceTypes: List<String>,
    val urls: List<String>
)

class RealWeb3NamesRepository(
    private val remoteStorageSource: StorageDataSource,
    private val web3NamesServiceChainIdProvider: Web3NamesServiceChainIdProvider,
    private val transferRecipientApi: TransferRecipientsApi,
    private val caip19MatcherFactory: Caip19MatcherFactory,
    private val caip19Parser: Caip19Parser
) : Web3NamesRepository {

    override suspend fun queryWeb3NameAccount(web3Name: String, chain: Chain, chainAsset: Chain.Asset): List<Web3NameAccount> {
        val owner = getWeb3NameAccountOwner(web3Name) ?: return emptyList()
        val serviceEndpoints = getDidServiceEndpoints(owner)
        val transferRecipientEndpoint = serviceEndpoints.firstKiltTransferAssetRecipientV1Endpoint() ?: return emptyList()
        val transferRecipientUrl = transferRecipientEndpoint.urls.first()

        return getChainRecipients(transferRecipientUrl, chain, chainAsset)
    }

    private suspend fun getWeb3NameAccountOwner(web3Name: String): AccountId? {
        return remoteStorageSource.query(web3NamesServiceChainIdProvider.getChainId()) {
            runtime.metadata
                .module("Web3Names")
                .storage("Owner")
                .query(web3Name.toByteArray(), binding = ::bindOwners)
        }
    }

    private suspend fun getDidServiceEndpoints(accountId: AccountId): List<ServiceEndpoint> {
        val serviceEndpoints = remoteStorageSource.query(web3NamesServiceChainIdProvider.getChainId()) {
            runtime.metadata.module("Did")
                .storage("ServiceEndpoints")
                .entries(
                    accountId,
                    keyExtractor = { it },
                    binding = { data, _ -> bindEndpoint(data) }
                )
        }

        return serviceEndpoints.values.toList()
    }

    private fun bindOwners(data: Any?): AccountId? {
        if (data == null) return null

        val ownerStruct = data.castToStruct()
        return ownerStruct.get<AccountId>("owner")
    }

    private fun bindEndpoint(data: Any?): ServiceEndpoint {
        val ownerStruct = data.castToStruct()

        val serviceTypes = bindList(ownerStruct["serviceTypes"]) { bindString(it) }
        val urls = bindList(ownerStruct["urls"]) { bindString(it) }

        return ServiceEndpoint(serviceTypes, urls)
    }

    private fun List<ServiceEndpoint>.firstKiltTransferAssetRecipientV1Endpoint(): ServiceEndpoint? {
        return firstOrNull {
            it.serviceTypes.contains(TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE)
        }
    }

    private suspend fun getChainRecipients(url: String, chain: Chain, chainAsset: Chain.Asset): List<Web3NameAccount> {
        val recipients = transferRecipientApi.getTransferRecipients(url)
        val caip19Matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)

        val chainRecipients = recipients.filterKeys {
            val caip19Identifier = caip19Parser.parseCaip19(it).getOrNull() ?: return@filterKeys false

            caip19Matcher.match(caip19Identifier)
        }

        return chainRecipients.flatMap { it.value }
            .map { Web3NameAccount(chain.accountIdOf(it.account), it.description) }
    }
}
