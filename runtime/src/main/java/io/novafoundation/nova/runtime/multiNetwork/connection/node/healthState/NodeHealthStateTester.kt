package io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState

interface NodeHealthStateTester {

    /**
     * Should return connection delay in ms
     */
    suspend fun testNodeHealthState(): Result<Long>
}
