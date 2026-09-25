package com.lucifer.hamrahyar.ui.theme.utils

object PlateUtils {
    /**
     * Cleans an Iranian vehicle plate string by:
     * 1. Removing hyphens and spaces
     * 2. Converting Persian/Arabic digits to English digits
     * 3. Mapping "الف" to "ا" (single character)
     */
    fun clean(plate: String): String {
        if (plate.isBlank()) return ""
        
        // Remove hyphens and spaces first
        val parts = plate.split("-", " ")
        
        return parts.joinToString("") { part ->
            if (part == "الف") {
                "ا"
            } else {
                part.map { c ->
                    when (c) {
                        // Persian digits
                        '\u06F0' -> '0'; '\u06F1' -> '1'; '\u06F2' -> '2'; '\u06F3' -> '3'; '\u06F4' -> '4'; '\u06F5' -> '5'; '\u06F6' -> '6'; '\u06F7' -> '7'; '\u06F8' -> '8'; '\u06F9' -> '9'
                        // Arabic digits
                        '\u0660' -> '0'; '\u0661' -> '1'; '\u0662' -> '2'; '\u0663' -> '3'; '\u0664' -> '4'; '\u0665' -> '5'; '\u0666' -> '6'; '\u0667' -> '7'; '\u0668' -> '8'; '\u0669' -> '9'
                        else -> c
                    }
                }.joinToString("")
            }
        }.replace("\\s+".toRegex(), "")
    }

    fun isPlateType(type: String?): Boolean {
        val t = type?.trim()?.lowercase()?.replace("-", "_") ?: return false
        return t == "plate" || t == "plate_vehicle" || t == "plate_motorcycle" || 
               t == "vehicle_plate" || t == "motorcycle_plate"
    }
}
