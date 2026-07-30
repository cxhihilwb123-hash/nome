package chat.simplex.common.model

/**
 * Tracks the latest native host-connectivity event so delayed Android recovery work cannot act on
 * a connection that has already recovered or on an older disconnect generation.
 */
internal class NomeCoreHostRecoveryState {
  private var generation = 0L
  private var disconnected = false

  fun hostStateChanged(connected: Boolean): Long? {
    generation += 1L
    disconnected = !connected
    return if (disconnected) generation else null
  }

  fun isCurrentDisconnect(token: Long): Boolean = disconnected && token == generation
}
