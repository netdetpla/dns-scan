package org.ndp.dns_scan_ns

import org.ndp.dns_scan_ns.bean.MQResult
import org.ndp.dns_scan_ns.utils.Logger.logger
import org.ndp.dns_scan_ns.utils.OtherTools
import org.ndp.dns_scan_ns.utils.RedisHandler
import org.xbill.DNS.Cache
import org.xbill.DNS.Lookup
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.Type
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class LookupTask(
    val domain: String,
    val dnsServer: String
) : Callable<LookupTask> {
    private val aRecordLookup = Lookup(domain, Type.A)
    private val resolver = SimpleResolver(dnsServer)

    init {
        resolver.setTimeout(5)
        aRecordLookup.setResolver(resolver)
        aRecordLookup.setCache(Cache())
    }

    override fun call(): LookupTask {
        aRecordLookup.run()
        return this
    }

    fun dnsServer(): String? {
        return if (aRecordLookup.result == Lookup.SUCCESSFUL) {
            dnsServer
        } else {
            null
        }
    }
}

object Main {

    private val task = RedisHandler.consumeTaskParam(
        RedisHandler.generateNonce(5)
    )
    private val targets = ArrayList<String>()

    private fun parseParam() {
        val param = task!!.param
        logger.debug("params: ")
        logger.debug(param)
        for (i in param.split(",")) {
            when {
                i.contains('-') -> targets.addAll(OtherTools.splitINetSegment(i))
                i.contains('/') -> targets.addAll(OtherTools.splitMaskedINet(i))
                else -> targets.add(i)
            }
        }
    }

    private fun execute(): List<String> {
        val d = "www.baidu.com"
        val executor = Executors.newFixedThreadPool(256)
        val results = ArrayList<Future<LookupTask>>()
        val dnsServers = ArrayList<String>()
        for (s in targets) {
            results.add(executor.submit(LookupTask(d, s)))
        }
        for (r in results) {
            val ds = r.get()
            ds.dnsServer()?.let { dnsServers.add(ds.dnsServer) }
        }
        executor.shutdown()
        return dnsServers
    }

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("ip-test started")
        if (task == null || task.taskID == 0) {
            logger.warn("no task, exiting...")
            return
        }
        // 执行
        try {
            parseParam()
            val results = execute()
            RedisHandler.produceResult(
                MQResult(task.taskID, results, 0, "")
            )
        } catch (e: Exception) {
            logger.error(e.toString())
            val stringWriter = StringWriter()
            e.printStackTrace(PrintWriter(stringWriter))
            RedisHandler.produceResult(
                MQResult(task.taskID, ArrayList(), 1, stringWriter.buffer.toString())
            )
        }
        // 结束
        logger.info("dns-scan-ns end successfully")
    }
}