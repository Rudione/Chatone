package io.rudione.chatone.util.network

import io.rudione.chatone.util.system.appForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn

data class NetworkStatus(
    val available: Boolean? = null,
    val route: String = "",
    val wakeVersion: Long = 0,
    val routeRevision: Long = 0
)

class NetworkMonitor(scope: CoroutineScope, changes: Flow<NetworkStatus> = observeNetworkStatus()) {
    val status = changes.stateIn(scope, SharingStarted.WhileSubscribed(5_000), NetworkStatus())
}

internal expect fun observeNetworkStatus(): Flow<NetworkStatus>

internal fun Flow<NetworkStatus>.withAppResumes(): Flow<NetworkStatus> = combine(
    this,
    appForeground.drop(1).filter { it }.runningFold(0L) { version, _ -> version + 1 }
) { path, version -> path.copy(wakeVersion = version) }.distinctUntilChanged()
