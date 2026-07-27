package dev.opentunnel.vpn.util

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/** An IP address plus prefix length, in the form VpnService.Builder wants. */
data class Cidr(val address: String, val prefixLength: Int, val isIpv6: Boolean)

object Net {

    /**
     * Accepts the shapes openconnect hands back:
     *   "10.0.0.5" + "255.255.255.0"   (address + dotted netmask)
     *   "10.0.0.0/24"                  (already CIDR)
     *   "2001:db8::1/64"
     *   "192.168.1.7"                  (bare host — treated as /32 or /128)
     */
    fun parseCidr(raw: String, netmask: String? = null): Cidr? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        val slash = value.indexOf('/')
        if (slash >= 0) {
            val host = value.substring(0, slash).trim()
            val suffix = value.substring(slash + 1).trim()
            if (host.isEmpty()) return null
            val ipv6 = host.contains(':')
            // Some gateways send "10.0.0.0/255.255.255.0".
            val prefix = suffix.toIntOrNull() ?: maskToPrefix(suffix) ?: return null
            if (!validPrefix(prefix, ipv6)) return null
            return Cidr(host, prefix, ipv6)
        }

        val ipv6 = value.contains(':')
        val prefix = when {
            netmask.isNullOrBlank() -> if (ipv6) 128 else 32
            netmask.toIntOrNull() != null -> netmask.toInt()
            else -> maskToPrefix(netmask) ?: if (ipv6) 128 else 32
        }
        if (!validPrefix(prefix, ipv6)) return null
        return Cidr(value, prefix, ipv6)
    }

    private fun validPrefix(prefix: Int, ipv6: Boolean) =
        prefix in 0..(if (ipv6) 128 else 32)

    /** "255.255.255.0" -> 24. Returns null when the mask is not contiguous. */
    fun maskToPrefix(mask: String): Int? {
        val parts = mask.trim().split('.')
        if (parts.size != 4) return null
        var bits = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            bits = (bits shl 8) or octet.toLong()
        }
        // Must be a run of ones followed by a run of zeros.
        val inverted = bits.inv() and 0xFFFFFFFFL
        if ((inverted + 1) and inverted != 0L) return null
        return java.lang.Long.bitCount(bits and 0xFFFFFFFFL)
    }

    fun isValidIp(value: String): Boolean = runCatching {
        val addr = InetAddress.getByName(value)
        addr is Inet4Address || addr is Inet6Address
    }.getOrDefault(false)

    /**
     * RFC1918 + link-local + CGNAT + IPv6 ULA/link-local. Excluded from the
     * tunnel when "keep local network traffic off the VPN" is on, so printers,
     * NAS boxes and Chromecasts keep working while connected.
     */
    val LOCAL_NETWORKS: List<Cidr> = listOf(
        Cidr("10.0.0.0", 8, false),
        Cidr("172.16.0.0", 12, false),
        Cidr("192.168.0.0", 16, false),
        Cidr("169.254.0.0", 16, false),
        Cidr("100.64.0.0", 10, false),
        Cidr("fc00::", 7, true),
        Cidr("fe80::", 10, true),
    )

    /**
     * Subtracts [excluded] from 0.0.0.0/0, producing the largest set of prefixes
     * that covers everything else. VpnService has no "exclude this route" API
     * below API 33, so a default route has to be expressed as its complement.
     */
    fun ipv4DefaultMinus(excluded: List<Cidr>): List<Cidr> {
        var remaining = listOf(Cidr("0.0.0.0", 0, false))
        for (block in excluded.filter { !it.isIpv6 }) {
            remaining = remaining.flatMap { subtract(it, block) }
        }
        return remaining
    }

    private fun subtract(from: Cidr, remove: Cidr): List<Cidr> {
        val fromStart = ipv4ToLong(from.address) ?: return listOf(from)
        val removeStart = ipv4ToLong(remove.address) ?: return listOf(from)
        val fromEnd = fromStart + size(from.prefixLength) - 1
        val removeEnd = removeStart + size(remove.prefixLength) - 1

        // No overlap.
        if (removeEnd < fromStart || removeStart > fromEnd) return listOf(from)
        // Fully covered.
        if (removeStart <= fromStart && removeEnd >= fromEnd) return emptyList()

        // Split in half and recurse; every CIDR block splits cleanly in two.
        val childPrefix = from.prefixLength + 1
        if (childPrefix > 32) return emptyList()
        val half = size(childPrefix)
        val lower = Cidr(longToIpv4(fromStart), childPrefix, false)
        val upper = Cidr(longToIpv4(fromStart + half), childPrefix, false)
        return subtract(lower, remove) + subtract(upper, remove)
    }

    private fun size(prefix: Int): Long = 1L shl (32 - prefix)

    fun ipv4ToLong(address: String): Long? {
        val parts = address.split('.')
        if (parts.size != 4) return null
        var value = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            value = (value shl 8) or octet.toLong()
        }
        return value
    }

    fun longToIpv4(value: Long): String =
        "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
}
