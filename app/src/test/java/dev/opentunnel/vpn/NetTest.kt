package dev.opentunnel.vpn

import dev.opentunnel.vpn.util.Net
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetTest {

    @Test
    fun testParseCidrDottedMask() {
        val cidr = Net.parseCidr("10.0.0.5", "255.255.255.0")
        assertNotNull(cidr)
        assertEquals("10.0.0.5", cidr?.address)
        assertEquals(24, cidr?.prefixLength)
        assertEquals(false, cidr?.isIpv6)
    }

    @Test
    fun testParseCidrSlashNotation() {
        val cidr = Net.parseCidr("192.168.1.0/24")
        assertNotNull(cidr)
        assertEquals("192.168.1.0", cidr?.address)
        assertEquals(24, cidr?.prefixLength)
        assertEquals(false, cidr?.isIpv6)
    }

    @Test
    fun testParseCidrIpv6() {
        val cidr = Net.parseCidr("2001:db8::1/64")
        assertNotNull(cidr)
        assertEquals("2001:db8::1", cidr?.address)
        assertEquals(64, cidr?.prefixLength)
        assertEquals(true, cidr?.isIpv6)
    }

    @Test
    fun testMaskToPrefix() {
        assertEquals(24, Net.maskToPrefix("255.255.255.0"))
        assertEquals(16, Net.maskToPrefix("255.255.0.0"))
        assertEquals(8, Net.maskToPrefix("255.0.0.0"))
        assertEquals(32, Net.maskToPrefix("255.255.255.255"))
        assertNull(Net.maskToPrefix("255.0.255.0"))
    }

    @Test
    fun testIpv4DefaultMinus() {
        val excluded = listOf(Net.parseCidr("192.168.1.0/24")!!)
        val routes = Net.ipv4DefaultMinus(excluded)
        assertTrue(routes.isNotEmpty())
    }
}
