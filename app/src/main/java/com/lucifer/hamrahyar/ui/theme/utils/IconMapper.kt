package com.lucifer.hamrahyar.ui.theme.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun map(icon: String?): ImageVector {
        return when (icon?.lowercase()) {
            "edit", "📝" -> Icons.Rounded.Edit
            "translate", "🔤" -> Icons.Rounded.Translate
            "description", "📄" -> Icons.Rounded.Description
            "language", "🌐" -> Icons.Rounded.Language
            "pictureaspdf" -> Icons.Rounded.PictureAsPdf
            "insertdrivefile" -> Icons.AutoMirrored.Rounded.InsertDriveFile
            "viewagenda" -> Icons.Rounded.ViewAgenda
            "tablechart", "📊" -> Icons.Rounded.TableChart
            "calculate" -> Icons.Rounded.Calculate
            "editnote" -> Icons.Rounded.EditNote
            "mail" -> Icons.Rounded.Mail
            "listalt", "📋" -> Icons.AutoMirrored.Rounded.ListAlt
            "accountbalance", "🏛️" -> Icons.Rounded.AccountBalance
            "school", "🎓", "🏫" -> Icons.Rounded.School
            "assignment", "📜" -> Icons.AutoMirrored.Rounded.Assignment
            "public" -> Icons.Rounded.Public
            "accountbalancewallet", "💰" -> Icons.Rounded.AccountBalanceWallet
            "payments", "💳" -> Icons.Rounded.Payments
            "verifieduser", "🛡️" -> Icons.Rounded.VerifiedUser
            "menubook", "📚" -> Icons.AutoMirrored.Rounded.MenuBook
            "event", "📅" -> Icons.Rounded.Event
            "flight", "✈️" -> Icons.Rounded.Flight
            "badge", "🪪" -> Icons.Rounded.Badge
            "security" -> Icons.Rounded.Security
            "creditcard" -> Icons.Rounded.CreditCard
            "receipt", "🧾" -> Icons.Rounded.Receipt
            "directionscar", "🚗", "🚓", "🚔" -> Icons.Rounded.DirectionsCar
            "locationcity", "🏙️" -> Icons.Rounded.LocationCity
            "swaphoriz", "🔄" -> Icons.Rounded.SwapHoriz
            "confirmationnumber", "🎟️" -> Icons.Rounded.ConfirmationNumber
            "eventavailable" -> Icons.Rounded.EventAvailable
            "search", "🔍" -> Icons.Rounded.Search
            "attachmoney" -> Icons.Rounded.AttachMoney
            "toll", "🛣️" -> Icons.Rounded.Toll
            "shield" -> Icons.Rounded.Shield
            "brush", "🎨", "🖌️" -> Icons.Rounded.Brush
            "image", "🖼️" -> Icons.Rounded.Image
            "photo" -> Icons.Rounded.Photo
            "restaurantmenu" -> Icons.Rounded.RestaurantMenu
            "android", "🤖" -> Icons.Rounded.Android
            "favorite" -> Icons.Rounded.Favorite
            "work" -> Icons.Rounded.Work
            "removecircle" -> Icons.Rounded.RemoveCircle
            "list" -> Icons.AutoMirrored.Rounded.List
            "class" -> Icons.Rounded.Class
            "assessment" -> Icons.Rounded.Assessment
            "autoawesome" -> Icons.Rounded.AutoAwesome
            "create", "✍️" -> Icons.Rounded.Create
            "slideshow" -> Icons.Rounded.Slideshow
            "autofixhigh" -> Icons.Rounded.AutoFixHigh
            "computer", "💻" -> Icons.Rounded.Computer
            "smartphone", "📱" -> Icons.Rounded.Smartphone
            "localhospital", "🏥" -> Icons.Rounded.LocalHospital
            "business", "🏢" -> Icons.Rounded.Business
            else -> Icons.Rounded.Category
        }
    }
    
    fun mapCategory(title: String): ImageVector {
        return when {
            title.contains("دولت") -> Icons.Rounded.AccountBalance
            title.contains("بانک") -> Icons.Rounded.Payments
            title.contains("خودرو") || title.contains("نقلیه") -> Icons.Rounded.DirectionsCar
            title.contains("طراحی") -> Icons.Rounded.Brush
            title.contains("هوش مصنوعی") -> Icons.Rounded.Android
            title.contains("آموزش") -> Icons.Rounded.School
            title.contains("تایپ") -> Icons.Rounded.Keyboard
            title.contains("بیمه") -> Icons.Rounded.Shield
            else -> Icons.Rounded.Category
        }
    }
}
