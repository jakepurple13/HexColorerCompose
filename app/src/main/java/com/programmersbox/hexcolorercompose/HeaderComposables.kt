package com.programmersbox.hexcolorercompose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun Header(
    mainModel: MainModel,
    settingsDialogModel: SettingsDialogModel,
    fontColor: Color,
    animatedBackground: Color,
    clipboardManager: ClipboardManager,
    scope: CoroutineScope,
    savedColors: List<ColorItem>,
    scaffoldState: BottomSheetScaffoldState,
    dao: ColorDao,
    showSnackBar: (String) -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "#${mainModel.hexColor}",
                color = fontColor,
                fontSize = 45.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onLongClick = {
                            clipboardManager.getText()
                                ?.let { t ->
                                    if ("^#(?:[0-9a-fA-F]{3}){1,2}$".toRegex().matches(t.text))
                                        t.text.removePrefix("#")
                                    else null
                                }
                                ?.let {
                                    mainModel.hexColor = it
                                    showSnackBar("Pasted: $it")
                                }
                        },
                        onClick = {},
                        onDoubleClick = {
                            clipboardManager.setText(AnnotatedString("#${mainModel.hexColor}", ParagraphStyle()))
                            showSnackBar("Copied")
                        }
                    )
            )
        },
        navigationIcon = {
            val isSaved = savedColors.any { it.color == mainModel.hexColor }
            IconButton(
                onClick = {
                    scope.launch {
                        if (isSaved) {
                            savedColors.find { it.color == mainModel.hexColor }
                                ?.let { dao.deleteColor(it) }
                                ?.let { showSnackBar("Deleted: ${mainModel.hexColor}") }
                        } else if (mainModel.hexColor.length == 6) {
                            dao.insertColor(ColorItem(color = mainModel.hexColor))
                            showSnackBar("Saved: ${mainModel.hexColor}")
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = fontColor,
                    modifier = Modifier.rotate(animateFloatAsState(targetValue = if (isSaved) 135f else 0f).value)
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    mainModel.hexColor = Integer.toHexString(
                        Color(
                            red = Random.nextInt(0, 255),
                            green = Random.nextInt(0, 255),
                            blue = Random.nextInt(0, 255)
                        ).toArgb()
                    ).drop(2)
                }
            ) { Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = fontColor) }

            IconButton(
                onClick = {
                    scope.launch {
                        if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                        else scaffoldState.bottomSheetState.collapse()
                    }
                }
            ) { Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = fontColor) }

            IconButton(
                onClick = { settingsDialogModel.showSettings = true }
            ) { Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = fontColor) }
        },
        backgroundColor = animatedBackground
    )
}