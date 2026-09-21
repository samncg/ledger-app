package com.ledger.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ledger.app.ui.t
import com.ledger.app.util.decodeReceipt

/* Receipt photos — thumbnail + full-screen viewer shared by the log form and history. */

/** Small receipt thumbnail decoded from its data URL; optionally tappable. */
@Composable
fun ReceiptThumbnail(receipt: String?, size: Int = 40, onClick: (() -> Unit)? = null) {
    if (receipt.isNullOrBlank()) return
    val bmp = remember(receipt) { decodeReceipt(receipt)?.asImageBitmap() } ?: return
    Image(
        bitmap = bmp,
        contentDescription = t("log.receipt"),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

/** Full-screen receipt viewer. */
@Composable
fun ReceiptPreviewDialog(receipt: String?, onClose: () -> Unit) {
    if (receipt.isNullOrBlank()) return
    val bmp = remember(receipt) { decodeReceipt(receipt)?.asImageBitmap() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center,
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = t("log.receipt"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Outlined.Close, t("app.close"), tint = Color.White)
            }
        }
    }
}
