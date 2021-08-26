package com.programmersbox.hexcolorercompose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@Composable
fun Drawer(
    savedColors: List<ColorItem>,
    animatedBackground: Color,
    fontColor: Color,
    model: MainModel,
    scope: CoroutineScope,
    dao: ColorDao
) {

    if (model.showDeletePopup && model.deleteColor != null) {

        val onDismiss = { model.showDeletePopup = false }

        val c = Color("#${model.deleteColor!!.color}".toColorInt())
        val font = if (c.luminance() > .5f) Color.Black else Color.White

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Remove #${model.deleteColor!!.color}", color = font) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        scope.launch(Dispatchers.IO) { dao.deleteColor(model.deleteColor!!) }
                    },
                    colors = ButtonDefaults.textButtonColors(backgroundColor = c)
                ) { Text("Yes", color = font) }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(backgroundColor = c)
                ) { Text("No", color = font) }
            },
            backgroundColor = c
        )

    }

    val c1 = if (savedColors.isNotEmpty()) savedColors.map { Color("#${it.color}".toColorInt()) }
    else listOf(MaterialTheme.colors.surface, MaterialTheme.colors.surface)
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.background(Brush.verticalGradient(c1))
    ) {
        item {
            TopAppBar(backgroundColor = animatedBackground) {
                Text(
                    "Saved Colors: ${savedColors.size}",
                    fontSize = 45.sp,
                    textAlign = TextAlign.Center,
                    color = fontColor,
                )
            }
        }

        items(savedColors) {
            val c = Color("#${it.color}".toColorInt())
            Text(
                "#${it.color}",
                fontSize = 45.sp,
                textAlign = TextAlign.Center,
                color = if (c.luminance() > .5f) Color.Black else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onLongClick = {
                            model.deleteColor = it
                            model.showDeletePopup = true
                        },
                        onClick = { model.hexColor = it.color },
                        onDoubleClick = {
                            model.hexColor = it.color
                            model.deleteColor = it
                            model.showDeletePopup = true
                        }
                    )
                    .background(c)
            )
        }
    }
}