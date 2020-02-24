package org.ndp.dns_scan_ns.utils

import java.util.*

object OtherTools {

    fun iNetString2Number(ipStr: String): Long {
        return Arrays.stream(ipStr.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            .map { java.lang.Long.parseLong(it) }
            .reduce(0L) { x, y -> (x!! shl 8) + y!! }
    }

    fun iNetNumber2String(ipLong: Long): String {
        var origin = ipLong
        val segments = ArrayList<String>()
        for (i in 0..3) {
            segments.add((origin % 256L).toString())
            origin /= 256
        }
        segments.reverse()
        return segments.joinToString(".")
    }

    private fun parseIPStart(ip: Long, mask: Int): Long {
        val startMask = ((1 shl 32 - mask) - 1).toLong().inv()
        return ip and startMask
    }

    private fun parseIPEnd(ipStart: Long, mask: Int): Long {
        val endMask = ((1 shl 32 - mask) - 1).toLong()
        return ipStart or endMask
    }

    fun splitMaskedINet(ipSegment: String): List<String> {
        val ipSet = ipSegment.split("/")
        val mask = ipSet[1].toInt()
        val ipStart = parseIPStart(iNetString2Number(ipSet[0]), mask)
        val ipEnd = parseIPEnd(ipStart, mask)
        val targets = ArrayList<String>()
        for (i in ipStart..ipEnd) {
            targets.add(iNetNumber2String(i))
        }
        return targets
    }

    fun splitINetSegment(ipSegment: String): List<String> {
        val ipSet = ipSegment.split("-")
        val ipStart = iNetString2Number(ipSet[0])
        val ipEnd = iNetString2Number(ipSet[1])
        val targets = ArrayList<String>()
        for (i in ipStart..ipEnd) {
            targets.add(iNetNumber2String(i))
        }
        return targets
    }
}