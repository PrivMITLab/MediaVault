package com.example.ui.screens.photos

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.components.FileInfoDialog
import com.example.ui.theme.RoseError
import java.io.File

@Composable
fun PhotoViewerDialog(
    photos: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDeletePhoto: (MediaItem) -> Unit,
    onExportPhoto: (MediaItem) -> Unit,
    onRestorePhoto: ((MediaItem) -> Unit)? = null
) {
    val context = LocalContext.current
    BackHandler(onBack = onDismiss)

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
        pageCount = { photos.size }
    )

    var showInfoDialog by remember { mutableStateOf(false) }
    val currentPhoto = photos.getOrNull(pagerState.currentPage)

    if (showInfoDialog && currentPhoto != null) {
        FileInfoDialog(
            item = currentPhoto,
            onDismiss = { showInfoDialog = false },
            onExport = {
                onExportPhoto(currentPhoto)
                showInfoDialog = false
            },
            onRestore = {
                onRestorePhoto?.invoke(currentPhoto)
                showInfoDialog = false
                if (photos.size <= 1) onDismiss()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale > 1f) {
                                    val maxOffsetX = (size.width * (scale - 1)) / 2
                                    val maxOffsetY = (size.height * (scale - 1)) / 2
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = photo.displayTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }

            // Top Overlay Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${pagerState.currentPage + 1} of ${photos.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            currentPhoto?.let { sharePhoto(context, it) }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            currentPhoto?.let {
                                onDeletePhoto(it)
                                if (photos.size <= 1) onDismiss()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                    }
                }
            }
        }
    }
}

private fun sharePhoto(context: Context, item: MediaItem) {
    try {
        val file = File(item.filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share from MediaVault"))
    } catch (_: Exception) {}
}
