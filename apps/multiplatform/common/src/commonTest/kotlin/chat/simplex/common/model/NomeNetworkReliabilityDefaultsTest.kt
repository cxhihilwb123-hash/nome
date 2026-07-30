package chat.simplex.common.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NomeNetworkReliabilityDefaultsTest {
  @Test
  fun directNetworkUsesProvenPrivateSmpHeartbeat() {
    assertNomeReliabilityDefaults(NetCfg.defaults)
  }

  @Test
  fun proxyNetworkUsesProvenPrivateSmpHeartbeat() {
    assertNomeReliabilityDefaults(NetCfg.proxyDefaults)
  }

  private fun assertNomeReliabilityDefaults(cfg: NetCfg) {
    assertEquals(120_000_000L, cfg.smpPingInterval)
    assertEquals(1, cfg.smpPingCount)
    assertTrue(cfg.enableKeepAlive)
  }
}
