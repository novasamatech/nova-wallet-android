package io.novafoundation.nova.web3names.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
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
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

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
        val owners = getWeb3NameAccountOwner(web3Name)
        val firstMatchedOwner = owners.first()

        val serviceEndpoints = getDidServiceEndpoints(firstMatchedOwner)
        val transferRecipientEndpoint = serviceEndpoints.firstKiltTransferAssetRecipientV1Endpoint() ?: return emptyList()
        val transferRecipientUrl = transferRecipientEndpoint.urls.first()

        return getChainRecipients(transferRecipientUrl, chain, chainAsset)
    }

    private suspend fun getWeb3NameAccountOwner(web3Name: String): List<AccountId> {
        return remoteStorageSource.query(
            chainId = web3NamesServiceChainIdProvider.getChainId(),
            keyBuilder = {
                it.metadata.module("Web3Names").storage("Owner").storageKey(it, web3Name)
            },
            binding = { scale, _ -> bindOwners(scale) }
        )
    }

    private suspend fun getDidServiceEndpoints(accountId: AccountId): List<ServiceEndpoint> {
        return remoteStorageSource.query(
            chainId = web3NamesServiceChainIdProvider.getChainId(),
            keyBuilder = {
                it.metadata.module("Did").storage("ServiceEndpoints").storageKey(it, accountId)
            },
            binding = { scale, _ -> bindEndpoints(scale) }
        )
    }

    private fun bindOwners(scale: String?): List<AccountId> {
        return buildList {
            bindList(scale) {
                bindList(it) {
                    val ownerStruct = it.castToStruct()
                    val ownerAccountId = ownerStruct.get<AccountId>("owner")
                    ownerAccountId?.let { add(it) }
                }
            }
        }
    }

    private fun bindEndpoints(scale: String?): List<ServiceEndpoint> {
        return buildList {
            bindList(scale) {
                bindList(it) {
                    val ownerStruct = it.castToStruct()
                    val serviceTypes = ownerStruct.get<List<String>>("serviceTypes") ?: emptyList()
                    val urls = ownerStruct.get<List<String>>("urls") ?: emptyList()
                    add(ServiceEndpoint(serviceTypes, urls))
                }
            }
        }
    }

    private fun List<ServiceEndpoint>.firstKiltTransferAssetRecipientV1Endpoint(): ServiceEndpoint? {
        return firstOrNull {
            it.serviceTypes.contains("KiltTransferAssetRecipientV1")
        }
    }

    private suspend fun getChainRecipients(url: String, chain: Chain, chainAsset: Chain.Asset): List<Web3NameAccount> {
        val recipients = transferRecipientApi.getTransferRecipients(url)
        val caip19Matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)

        val chainRecipients = recipients.filterKeys {
            val caip19Identifier = caip19Parser.parseCaip19(it).getOrNull()
            if (caip19Identifier == null) {
                false
            } else {
                caip19Matcher.match(caip19Identifier)
            }
        }

        return chainRecipients.flatMap { it.value }
            .map { Web3NameAccount(chain.accountIdOf(it.account), it.description) }
    }
}
