package com.lucifer.hamrahyar.ui.theme.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.ContactSupport
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object StyleMapper {
    
    data class Style(val icon: ImageVector, val color: Color)

    fun getCategoryStyle(title: String): Style {
        return when {
            title.contains("دولت") || title.contains("عمومی") -> Style(Icons.Rounded.AccountBalance, Color(0xFF0984E3))
            title.contains("بانک") || title.contains("مالی") -> Style(Icons.Rounded.AccountBalanceWallet, Color(0xFF00B894))
            title.contains("خودرو") || title.contains("نقلیه") || title.contains("وسیله") -> Style(Icons.Rounded.DirectionsCar, Color(0xFFE17055))
            title.contains("طراحی") || title.contains("هنر") -> Style(Icons.Rounded.Brush, Color(0xFF6C5CE7))
            title.contains("هوش") || title.contains("تکنولوژی") -> Style(Icons.Rounded.Memory, Color(0xFF00CEC9))
            title.contains("آموزش") || title.contains("درس") || title.contains("دانشگاه") -> Style(Icons.Rounded.School, Color(0xFFFD9644))
            title.contains("بیمه") || title.contains("سلامت") -> Style(Icons.Rounded.HealthAndSafety, Color(0xFFD63031))
            title.contains("تایپ") || title.contains("نگارش") -> Style(Icons.Rounded.Keyboard, Color(0xFF2D3436))
            else -> Style(Icons.Rounded.GridView, Color(0xFF636E72))
        }
    }

    fun getServiceStyle(title: String, categoryColor: Color): Style {
        val icon = when {
            // --- خودرو و تردد (اولویت بالا برای نمایش صحیح آیکون‌های تخصصی) ---
            title.contains("خلافی") || title.contains("جریمه") -> Icons.Rounded.Gavel
            title.contains("معاینه") || title.contains("فنی") -> Icons.Rounded.BuildCircle
            title.contains("پلاک") || title.contains("خودرو") -> Icons.Rounded.DirectionsCar
            title.contains("سوخت") || title.contains("بنزین") -> Icons.Rounded.LocalGasStation
            title.contains("نقشه") || title.contains("مسیریاب") -> Icons.Rounded.Map
            title.contains("گذرنامه") || title.contains("پاسپورت") -> Icons.Rounded.Badge

            // --- بیمه و سلامت ---
            title.contains("بیمه") || title.contains("تأمین اجتماعی") || title.contains("سلامت") -> Icons.Rounded.HealthAndSafety

            // --- مالی و تجاری ---
            title.contains("پرداخت") || title.contains("واریز") -> Icons.Rounded.AccountBalanceWallet
            title.contains("خرید") || title.contains("سفارش") -> Icons.Rounded.ShoppingCart
            title.contains("فروش") || title.contains("واگذاری") -> Icons.Rounded.Sell
            title.contains("مالیات") || title.contains("دارایی") -> Icons.Rounded.RequestQuote
            title.contains("بورس") || title.contains("سهام") -> Icons.AutoMirrored.Rounded.ShowChart
            title.contains("چک") || title.contains("سفته") -> Icons.Rounded.Payments

            // --- اداری و ثبت نام ---
            title.contains("ثبت نام") || title.contains("عضویت") -> Icons.Rounded.PersonAdd
            title.contains("گواهی") || title.contains("مدرک") || title.contains("سند") -> Icons.AutoMirrored.Rounded.Article
            title.contains("پروانه") || title.contains("مجوز") -> Icons.Rounded.AssignmentInd
            title.contains("نوبت") || title.contains("رزرو") -> Icons.Rounded.EventAvailable
            title.contains("نامه") || title.contains("درخواست") -> Icons.Rounded.Mail
            title.contains("کارت ورود به جلسه") || title.contains("کارت آزمون") -> Icons.Rounded.Badge
            
            // --- ارتباطات و محتوا ---
            title.contains("ترجمه") || title.contains("زبان") -> Icons.Rounded.Translate
            title.contains("تایپ") || title.contains("نوشتن") -> Icons.Rounded.Keyboard
            title.contains("طراحی") || title.contains("ادیت") || title.contains("لوگو") -> Icons.Rounded.AutoFixHigh
            title.contains("صوت") || title.contains("گویش") || title.contains("گفتار") -> Icons.Rounded.SettingsVoice
            title.contains("فیلم") || title.contains("ویدیو") -> Icons.Rounded.Movie
            title.contains("عکس") || title.contains("تصویر") -> Icons.Rounded.Image
            
            // --- جستجو و پیگیری ---
            title.contains("استعلام") || title.contains("جستجو") || title.contains("پیگیری") -> Icons.Rounded.Search
            title.contains("دریافت") || title.contains("دانلود") -> Icons.Rounded.FileDownload
            title.contains("تبدیل") || title.contains("تغییر") -> Icons.Rounded.SwapHoriz
            
            // --- موارد عمومی ---
            title.contains("حذف") || title.contains("ابطال") -> Icons.Rounded.DeleteSweep
            title.contains("راهنما") || title.contains("مشاوره") -> Icons.AutoMirrored.Rounded.ContactSupport
            title.contains("امنیت") || title.contains("رمز") -> Icons.Rounded.VpnKey
            
            else -> Icons.Rounded.Layers
        }
        
        return Style(icon, categoryColor)
    }
}
