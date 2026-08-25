package com.freeiperf3client.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultFormattingTest {
    @Test
    fun malformedIpv4IsRejectedImmediately() {
        val result = validateConfig("192.0.2.999", "5201", "10", "50")

        assertFalse(result.valid)
        assertEquals("That is not a valid IP address or hostname", result.hostError)
    }

    @Test
    fun endpointCheckDoesNotDependOnThroughputSettings() {
        val result = validateEndpoint("iperf.example.test", "5201")

        assertTrue(result.valid)
        assertNull(result.durationError)
        assertNull(result.udpError)
    }

    @Test
    fun validationAcceptsIpv6AndRejectsShellLikeInput() {
        assertTrue(isValidServerName("2001:db8::1"))
        assertFalse(isValidServerName("server.example;reboot"))
        assertFalse(isValidServerName("server example"))
    }

    @Test
    fun udpScorePenalizesPacketLoss() {
        val config = TestConfig("example.test", 5201, 10, 50)
        val perfect = TestResult(
            mode = TestMode.UDP_DOWNLOAD,
            connection = null,
            downloadBitsPerSecond = 50_000_000.0,
            jitterMs = 1.0,
            lossPercent = 0.0,
            rawOutput = "",
        )
        val lossy = perfect.copy(lossPercent = 3.0)

        assertEquals(100, scoreUdp(perfect, config).first)
        assertTrue(scoreUdp(lossy, config).first < scoreUdp(perfect, config).first)
    }

    @Test
    fun privacySafeTextRedactsEveryServerOccurrence() {
        val config = TestConfig("private.example", 5201, 10, 50)
        val redacted = redactSensitiveText(
            "iperf3 -c private.example; remote_host=private.example",
            config,
        )

        assertFalse(redacted.contains("private.example"))
        assertEquals(2, "<redacted-server>".toRegex().findAll(redacted).count())
    }

    @Test
    fun discoveryScansTheLocalSlash24WithoutNetworkOrBroadcast() {
        val candidates = subnetCandidates("192.0.2.44", 24)

        assertEquals(253, candidates.size)
        assertTrue("192.0.2.1" in candidates)
        assertTrue("192.0.2.254" in candidates)
        assertFalse("192.0.2.0" in candidates)
        assertFalse("192.0.2.44" in candidates)
        assertFalse("192.0.2.255" in candidates)
    }

    @Test
    fun discoveryBoundsBroadNetworksToTheDevicesLocalSlash24() {
        val candidates = subnetCandidates("198.51.100.67", 16)

        assertTrue(candidates.all { it.startsWith("198.51.100.") })
        assertEquals(253, candidates.size)
    }
}
