package com.lucifer.hamrahyar.ui.theme.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

object SupportIntentHandler {

    private val RUBIKA_PACKAGES = listOf(
        "ir.resaneh1.ipt",
        "ir.resana.messenger",
        "ir.resana.app.rubika"
    )

    fun openSupport(context: Context, type: String, identifier: String) {
        if (identifier.isBlank()) {
            Toast.makeText(context, "اطلاعات تماس موجود نیست", Toast.LENGTH_SHORT).show()
            return
        }

        // Extract username if identifier is a URL
        val cleanIdentifier = when {
            identifier.contains("t.me/") -> identifier.substringAfter("t.me/").removePrefix("@").substringBefore("?").substringBefore("/")
            identifier.contains("eitaa.com/") -> identifier.substringAfter("eitaa.com/").removePrefix("@").substringBefore("?").substringBefore("/")
            identifier.contains("rubika.ir/") -> identifier.substringAfter("rubika.ir/").removePrefix("@").substringBefore("?").substringBefore("/")
            else -> identifier.removePrefix("@").trim()
        }
        
        when (type.lowercase()) {
            "telegram" -> openApp(
                context, 
                "tg://resolve?domain=$cleanIdentifier", 
                "https://t.me/$cleanIdentifier",
                listOf("org.telegram.messenger", "org.thunderdog.challegram", "org.telegram.plus", "ir.messenger.telegram")
            )
            "eitaa" -> openApp(
                context, 
                "eitaa://resolve?domain=$cleanIdentifier", 
                "https://eitaa.com/$cleanIdentifier",
                listOf("ir.eitaa.messenger", "ir.eitaa.messenger.b")
            )
            "rubika" -> {
                // Rubika handles links better through its own scheme or https
                openApp(
                    context,
                    "intent://rubika.ir/$cleanIdentifier#Intent;scheme=https;package=ir.resaneh1.ipt;end",
                    "https://rubika.ir/$cleanIdentifier",
                    RUBIKA_PACKAGES
                )
            }
            "email" -> openEmail(context, cleanIdentifier)
            "phone", "call" -> openPhone(context, cleanIdentifier)
            else -> {
                // If it looks like a URL, just try to open it
                if (identifier.startsWith("http")) {
                    openBrowser(context, identifier)
                } else {
                    Toast.makeText(context, "سرویس پشتیبانی نامعتبر", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPhone(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در برقراری تماس", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در باز کردن مرورگر", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openApp(context: Context, deepLink: String, fallbackUrl: String, packageNames: List<String>) {
        val packageManager = context.packageManager
        var targetPackage: String? = null

        // Find if any of the target apps are installed
        for (pkg in packageNames) {
            try {
                packageManager.getPackageInfo(pkg, 0)
                targetPackage = pkg
                break
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            if (targetPackage != null) {
                intent.setPackage(targetPackage)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Try fallback to web browser
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "خطا در باز کردن برنامه مقصد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openEmail(context: Context, email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "هیچ برنامه ایمیلی یافت نشد", Toast.LENGTH_SHORT).show()
        }
    }
}
