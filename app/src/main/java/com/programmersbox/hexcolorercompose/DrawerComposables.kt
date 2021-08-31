package com.programmersbox.hexcolorercompose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
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
    backColor: Color,
    model: MainModel,
    use3d: Boolean,
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

    Scaffold(
        topBar = {
            TopAppBar(backgroundColor = animatedBackground) {
                if (use3d) {
                    Text3D(text = "Saved Colors: ${savedColors.size}", fontColor = fontColor)
                } else {
                    Text(
                        "Saved Colors: ${savedColors.size}",
                        fontSize = 45.sp,
                        textAlign = TextAlign.Center,
                        color = fontColor,
                    )
                }
            }
        },
        backgroundColor = backColor,
    ) { p ->

        val size = 100.dp

        LazyVerticalGrid(
            cells = GridCells.Adaptive(size),
            contentPadding = p,
        ) {
            items(savedColors) {
                val c = remember { Color("#${it.color}".toColorInt()) }
                Surface(
                    modifier = Modifier
                        .size(size)
                        .padding(5.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
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
                        ),
                    color = c,
                    shape = CircleShape,
                    border = BorderStroke(
                        width = animateDpAsState(if (model.hexColor == it.color) 5.dp else 2.dp).value,
                        color = remember { if (c.luminance() > .5f) Color.Black else Color.White },
                    ),
                    elevation = 5.dp
                ) {
                    if (use3d) {
                        Text3D(
                            text = "#${it.color}",
                            fontColor = if (c.luminance() > .5f) Color.Black else Color.White,
                            fontSize = TextUnit.Unspecified
                        )
                    } else {
                        Box {
                            Text(
                                "#${it.color}",
                                textAlign = TextAlign.Center,
                                color = if (c.luminance() > .5f) Color.Black else Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}