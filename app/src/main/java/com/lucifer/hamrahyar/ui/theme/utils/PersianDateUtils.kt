package com.lucifer.hamrahyar.ui.theme.utils

import saman.zamani.persiandate.PersianDate
import saman.zamani.persiandate.PersianDateFormat

object PersianDateUtil {

    private val dateFormat = PersianDateFormat("Y/m/d")

    fun getDateText(currentTimeMillis: Long): String {
        val date = PersianDate(currentTimeMillis)
        val format = PersianDateFormat("Y/m/d")
        return format.format(date).replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
    }

    fun getDayText(currentTimeMillis: Long): String {
        val date = PersianDate(currentTimeMillis)
        return date.dayName()
    }

    fun parseIsoToPersianText(isoString: String?, includeTime: Boolean = true): String {
        if (isoString == null) return ""
        return try {
            val instant = kotlinx.datetime.Instant.parse(isoString)
            val date = PersianDate(instant.toEpochMilliseconds())
            val pattern = if (includeTime) "Y/m/d H:i" else "Y/m/d"
            val format = PersianDateFormat(pattern)
            format.format(date).replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")
        } catch (e: Exception) {
            ""
        }
    }
}
