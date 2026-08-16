package dev.opentunnel.vpn

import dev.opentunnel.vpn.core.AppMetadata
import dev.opentunnel.vpn.core.AppTrafficCollector
import dev.opentunnel.vpn.core.TrafficStatsProvider
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.data.AppTrafficEntry
import dev.opentunnel.vpn.data.SortDirection
import dev.opentunnel.vpn.data.SplitTunnelMode
import dev.opentunnel.vpn.data.TrafficFilterMode
import dev.opentunnel.vpn.data.TrafficSortBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.Collator

/**
 * Mock TrafficStatsProvider to simulate deterministic kernel counters and clock timestamps.
 */
class FakeTrafficStatsProvider : TrafficStatsProvider {
    val rxCounters = mutableMapOf<Int, Long>()
    val txCounters = mutableMapOf<Int, Long>()
    var currentTime: Long = 1000L
    var permissionGranted: Boolean = true

    override fun getUidRxBytes(uid: Int): Long = rxCounters.getOrDefault(uid, 0L)
    override fun getUidTxBytes(uid: Int): Long = txCounters.getOrDefault(uid, 0L)
    override fun getAllUidStats(): Map<Int, dev.opentunnel.vpn.core.UidTrafficBytes>? {
        val allUids = rxCounters.keys + txCounters.keys
        return allUids.associateWith { uid ->
            dev.opentunnel.vpn.core.UidTrafficBytes(
                rxBytes = rxCounters.getOrDefault(uid, 0L),
                txBytes = txCounters.getOrDefault(uid, 0L),
            )
        }
    }
    override fun getElapsedRealtime(): Long = currentTime
    override fun hasPermission(): Boolean = permissionGranted

    fun advanceTime(deltaMs: Long) {
        currentTime += deltaMs
    }

    fun setCounters(uid: Int, rx: Long, tx: Long) {
        rxCounters[uid] = rx
        txCounters[uid] = tx
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppTrafficCollectorTest {

    private lateinit var fakeStats: FakeTrafficStatsProvider
    private lateinit var testDispatcher: StandardTestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var collector: AppTrafficCollector

    private val chromeMeta = AppMetadata("com.android.chrome", 10001, "Google Chrome", false)
    private val telegramMeta = AppMetadata("org.telegram.messenger", 10002, "Telegram", false)
    private val spotifyMeta = AppMetadata("com.spotify.music", 10003, "Spotify", false)
    private val systemUiMeta = AppMetadata("com.android.systemui", 1000, "System UI", true)

    @Before
    fun setup() {
        fakeStats = FakeTrafficStatsProvider()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        collector = AppTrafficCollector(
            context = null,
            statsProvider = fakeStats,
            dispatcher = testDispatcher,
            sampleIntervalMs = 1000L,
        )
    }

    @Test
    fun testInitialBaselineEstablishment() = runTest(testDispatcher) {
        // Initial raw counters at boot
        fakeStats.setCounters(10001, 10_000_000L, 2_000_000L)
        fakeStats.setCounters(10002, 5_000_000L, 1_000_000L)

        collector.loadApps(listOf(chromeMeta, telegramMeta))
        collector.tick()

        val entries = collector.entries.value
        assertEquals(2, entries.size)

        val chrome = entries.first { it.packageName == chromeMeta.packageName }
        val telegram = entries.first { it.packageName == telegramMeta.packageName }

        // Session bytes should start at 0 relative to baseline
        assertEquals(0L, chrome.rxBytes)
        assertEquals(0L, chrome.txBytes)
        assertEquals(0L, chrome.totalBytes)
        assertEquals(0L, chrome.rxRate)
        assertEquals(0L, chrome.txRate)
        assertFalse(chrome.isActive)

        assertEquals(0L, telegram.totalBytes)
        assertEquals(0L, collector.summary.value.totalBytes)
    }

    @Test
    fun testByteDeltaAndSessionAccumulation() = runTest(testDispatcher) {
        fakeStats.setCounters(10001, 10_000_000L, 2_000_000L)
        collector.loadApps(listOf(chromeMeta))
        collector.tick() // baseline established at 10M / 2M

        // Simulate 1 second passing and 1MB download + 200KB upload
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 11_048_576L, 2_204_800L) // +1048576 B rx, +204800 B tx
        collector.tick()

        val chrome = collector.entries.value.first()
        assertEquals(1_048_576L, chrome.rxBytes)
        assertEquals(204_800L, chrome.txBytes)
        assertEquals(1_253_376L, chrome.totalBytes)
        assertTrue(chrome.rxRate > 0)
        assertTrue(chrome.txRate > 0)
        assertTrue(chrome.isActive)

        val summary = collector.summary.value
        assertEquals(1_048_576L, summary.totalRxBytes)
        assertEquals(204_800L, summary.totalTxBytes)
        assertEquals(1_253_376L, summary.totalBytes)
        assertEquals(1, summary.activeAppsCount)
    }

    @Test
    fun testEmaRateSmoothingAndZeroDecay() = runTest(testDispatcher) {
        fakeStats.setCounters(10001, 1_000_000L, 500_000L)
        collector.loadApps(listOf(chromeMeta))
        collector.tick()

        // Tick 1: sudden traffic spike of 1MB in 1 sec -> ~1,048,576 B/s
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 2_048_576L, 500_000L)
        collector.tick()

        val tick1Rate = collector.entries.value.first().rxRate
        assertEquals(1_048_576L, tick1Rate)

        // Tick 2: moderate traffic of 500KB in 1 sec -> smoothed EMA
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 2_548_576L, 500_000L)
        collector.tick()

        val tick2Rate = collector.entries.value.first().rxRate
        // EMA: 0.70 * 500_000 + 0.30 * 1_048_576 = 350_000 + 314_572 = 664_572
        assertTrue("Rate should be smoothed between 500k and 1M", tick2Rate in 600_000L..700_000L)

        // Tick 3: idle period 1 (0 delta) -> decay
        fakeStats.advanceTime(1000L)
        collector.tick()
        val tick3Rate = collector.entries.value.first().rxRate
        assertTrue("Rate should decay when idle", tick3Rate < tick2Rate)

        // Tick 4: idle period 2 (0 delta) -> full decay to 0
        fakeStats.advanceTime(1000L)
        collector.tick()
        val tick4Rate = collector.entries.value.first().rxRate
        assertEquals(0L, tick4Rate)
        assertFalse(collector.entries.value.first().isActive)
    }

    @Test
    fun testTrafficSharePercentage() = runTest(testDispatcher) {
        fakeStats.setCounters(10001, 0L, 0L)
        fakeStats.setCounters(10002, 0L, 0L)
        collector.loadApps(listOf(chromeMeta, telegramMeta))
        collector.tick()

        // Chrome generates 30MB, Telegram generates 10MB -> Total 40MB
        // Chrome share = 75%, Telegram share = 25%
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 30_000_000L, 0L)
        fakeStats.setCounters(10002, 10_000_000L, 0L)
        collector.tick()

        val entries = collector.entries.value
        val chrome = entries.first { it.packageName == chromeMeta.packageName }
        val telegram = entries.first { it.packageName == telegramMeta.packageName }

        assertEquals(75.0f, chrome.trafficSharePercent, 0.01f)
        assertEquals(25.0f, telegram.trafficSharePercent, 0.01f)
    }

    @Test
    fun testCounterWrapAndRebootHandling() = runTest(testDispatcher) {
        fakeStats.setCounters(10001, 100_000_000L, 50_000_000L)
        collector.loadApps(listOf(chromeMeta))
        collector.tick()

        // Advance traffic
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 110_000_000L, 55_000_000L)
        collector.tick()

        assertEquals(10_000_000L, collector.entries.value.first().rxBytes)

        // Simulate counter reset / device reboot (counter drops to small value 500k)
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 500_000L, 200_000L)
        collector.tick()

        // Should not crash, and counters should safely normalize
        val chrome = collector.entries.value.first()
        assertTrue(chrome.rxBytes >= 0L)
        assertTrue(chrome.txBytes >= 0L)
    }

    @Test
    fun testResetStatistics() = runTest(testDispatcher) {
        fakeStats.setCounters(10001, 0L, 0L)
        collector.loadApps(listOf(chromeMeta))
        collector.tick()

        // Generate 5MB traffic
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 5_000_000L, 0L)
        collector.tick()

        assertEquals(5_000_000L, collector.entries.value.first().rxBytes)

        // Reset
        collector.reset()

        val entriesAfterReset = collector.entries.value
        assertTrue(entriesAfterReset.isEmpty() || entriesAfterReset.all { it.totalBytes == 0L })
        assertEquals(0L, collector.summary.value.totalBytes)

        // Next tick: counters remain at 5M (so delta is 0)
        fakeStats.advanceTime(1000L)
        collector.tick()

        val chromeAfter = collector.entries.value.first()
        assertEquals(0L, chromeAfter.rxBytes)
        assertEquals(0L, chromeAfter.txBytes)
        assertEquals(0L, chromeAfter.totalBytes)
        assertEquals(0L, chromeAfter.rxRate)
    }

    @Test
    fun testSplitTunnelRoutingAttribution() = runTest(testDispatcher) {
        fakeStats.setCounters(10001, 0L, 0L)
        fakeStats.setCounters(10002, 0L, 0L)

        // Split tunnel: exclude Chrome
        collector.updateSettings(
            AppSettings(
                splitTunnelEnabled = true,
                splitTunnelMode = SplitTunnelMode.EXCLUDE_SELECTED,
                selectedPackages = setOf(chromeMeta.packageName),
            )
        )
        collector.loadApps(listOf(chromeMeta, telegramMeta))
        collector.tick()

        val entries = collector.entries.value
        val chrome = entries.first { it.packageName == chromeMeta.packageName }
        val telegram = entries.first { it.packageName == telegramMeta.packageName }

        assertFalse("Chrome is in exclude list, so should not be VPN routed", chrome.isVpnRouted)
        assertTrue("Telegram is not in exclude list, so should be VPN routed", telegram.isVpnRouted)
    }

    @Test
    fun testSortingAndFilteringLogic() {
        val entry1 = AppTrafficEntry(
            packageName = "com.google.chrome",
            uid = 10001,
            label = "Chrome",
            isSystem = false,
            isVpnRouted = true,
            rxBytes = 100_000L,
            txBytes = 20_000L,
            totalBytes = 120_000L,
            rxRate = 50_000L,
            txRate = 10_000L,
            totalRate = 60_000L,
            isActive = true,
        )

        val entry2 = AppTrafficEntry(
            packageName = "org.telegram.messenger",
            uid = 10002,
            label = "Telegram",
            isSystem = false,
            isVpnRouted = true,
            rxBytes = 200_000L,
            txBytes = 50_000L,
            totalBytes = 250_000L,
            rxRate = 10_000L,
            txRate = 5_000L,
            totalRate = 15_000L,
            isActive = true,
        )

        val entry3 = AppTrafficEntry(
            packageName = "com.android.systemui",
            uid = 1000,
            label = "System UI",
            isSystem = true,
            isVpnRouted = true,
            rxBytes = 0L,
            txBytes = 0L,
            totalBytes = 0L,
            rxRate = 0L,
            txRate = 0L,
            totalRate = 0L,
            isActive = false,
        )

        val list = listOf(entry1, entry2, entry3)

        // 1. Sort by Total Traffic Descending (Default)
        val sortedTotalDesc = list.sortedWith(compareByDescending<AppTrafficEntry> { it.totalBytes }.thenBy { it.label })
        assertEquals("Telegram", sortedTotalDesc[0].label)
        assertEquals("Chrome", sortedTotalDesc[1].label)
        assertEquals("System UI", sortedTotalDesc[2].label)

        // 2. Sort by Download Speed Descending
        val sortedSpeedDesc = list.sortedWith(compareByDescending<AppTrafficEntry> { it.rxRate }.thenBy { it.label })
        assertEquals("Chrome", sortedSpeedDesc[0].label) // 50k B/s
        assertEquals("Telegram", sortedSpeedDesc[1].label) // 10k B/s

        // 3. Filter by Active Only
        val activeOnly = list.filter { it.isActive || it.totalBytes > 0L }
        assertEquals(2, activeOnly.size)
        assertFalse(activeOnly.any { it.label == "System UI" })

        // 4. Filter by User Apps Only
        val userOnly = list.filter { !it.isSystem }
        assertEquals(2, userOnly.size)

        // 5. Search Query Filter
        val searchQuery = "tele"
        val searchResult = list.filter { it.label.contains(searchQuery, ignoreCase = true) }
        assertEquals(1, searchResult.size)
        assertEquals("Telegram", searchResult.first().label)
    }

    @Test
    fun testLatePermissionGrantNoSpike() = runTest(testDispatcher) {
        // App launches before permission is granted: counters are 0
        fakeStats.setCounters(10001, 0L, 0L)
        collector.loadApps(listOf(chromeMeta))
        collector.tick()

        var chrome = collector.entries.value.first { it.packageName == chromeMeta.packageName }
        assertEquals(0L, chrome.rxBytes)
        assertEquals(0L, chrome.rxRate)

        // Permission is now granted and system returns lifetime cumulative counters (e.g. 50 MB)
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 50_000_000L, 10_000_000L)
        collector.tick()

        chrome = collector.entries.value.first { it.packageName == chromeMeta.packageName }
        // Should establish baseline at 50MB and NOT register an artificial 50MB delta / 50MB/s speed spike
        assertEquals(0L, chrome.rxBytes)
        assertEquals(0L, chrome.rxRate)

        // Now Chrome actually downloads 100 KB in 1 second
        fakeStats.advanceTime(1000L)
        fakeStats.setCounters(10001, 50_100_000L, 10_000_000L)
        collector.tick()

        chrome = collector.entries.value.first { it.packageName == chromeMeta.packageName }
        assertEquals(100_000L, chrome.rxBytes)
        assertEquals(100_000L, chrome.rxRate)
        assertTrue(chrome.isActive)
    }
}
