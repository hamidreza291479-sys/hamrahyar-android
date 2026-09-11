package com.lucifer.hamrahyar.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir

@Composable
fun ExitDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {

        AlertDialog(

            onDismissRequest = onDismiss,

            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },

            title = {
                Text(
                    text = "خروج از برنامه",
                    fontFamily = Lalezar,
                    textAlign = TextAlign.Center
                )
            },

            text = {
                Text(
                    text = "آیا از خروج از برنامه مطمئن هستید؟",
                    fontFamily = Vazir,
                    textAlign = TextAlign.Center
                )
            },

            confirmButton = {

                TextButton(
                    onClick = onExit
                ) {
                    Text(
                        text = "خروج",
                        fontFamily = Lalezar,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = "انصراف",
                        fontFamily = Lalezar
                    )
                }
            }
        )
    }
}
