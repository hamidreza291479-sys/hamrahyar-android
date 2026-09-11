package com.lucifer.hamrahyar.ui.theme.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import java.util.TimeZone

object TimeManager {

    private const val NTP_SERVER = "time.google.com"

    private var timeOffset: Long = 0L

    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis() + timeOffset
    }

    suspend fun syncTime() {
        withContext(Dispatchers.IO) {
            try {
                val client = NTPUDPClient()
                client.defaultTimeout = 3000
                client.open()

                val address = InetAddress.getByName(NTP_SERVER)
                val info = client.getTime(address)
                info.computeDetails()

                val offset = info.offset ?: 0L
                timeOffset = offset

                client.close()
            } catch (_: Exception) {
                // fail silently
            }
        }
    }

    fun getTehranTimeZone(): TimeZone {
        return TimeZone.getTimeZone("Asia/Tehran")
    }
}