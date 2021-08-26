package com.programmersbox.hexcolorercompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.edit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.hexcolorercompose.ui.theme.HexColorerComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    @ExperimentalPermissionsApi
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = ColorDatabase.getInstance(this).colorDao()

        val dataStore = dataStore

        val history = dataStore.data.map { it[COLOR_HISTORY] ?: emptySet() }
        val favorites = dao
            .getAllColors()
            .map {
                it.sortedBy { c ->
                    val hsv = floatArrayOf(0f, 0f, 0f)
                    android.graphics.Color.colorToHSV(android.graphics.Color.parseColor("#${c.color}"), hsv)
                    hsv[0]
                }
            }

        setContent {

            HexColorerComposeTheme {

                val clipboardManager = LocalClipboardManager.current

                var backgroundColor by remember { mutableStateOf(Color.Black) }

                var hexColor by remember { mutableStateOf("") }

                var colorApi by remember { mutableStateOf<ColorApi?>(null) }

                val historyColors by history.collectAsState(initial = emptySet())

                val scope = rememberCoroutineScope()

                var showColorPickerPopup by remember { mutableStateOf(false) }

                LaunchedEffect(hexColor, showColorPickerPopup) {
                    backgroundColor = when (hexColor.length) {
                        6 -> Color("#$hexColor".toColorInt())
                        else -> Color.Black
                    }

                    if (hexColor.length == 6 && !showColorPickerPopup) {
                        scope.launch(Dispatchers.IO) {
                            if (hexColor !in historyColors) {
                                dataStore.edit {
                                    it[COLOR_HISTORY] = historyColors.toMutableList()
                                        .apply {
                                            add(0, hexColor)
                                            while (size > 5 && isNotEmpty()) removeLast()
                                        }
                                        .toSet()
                                }
                            }
                            colorApi = getColorApi(hexColor)
                        }
                    }
                }

                val onPress: (String) -> Unit = { if (hexColor.length < 6) hexColor += it }

                val fontColor = animateColorAsState(
                    if (backgroundColor.luminance() > .5f) Color.Black else Color.White,
                    animationSpec = tween(500)
                ).value

                val animatedBackground = animateColorAsState(backgroundColor, animationSpec = tween(500)).value

                val savedColors by favorites.collectAsState(initial = emptyList())

                val scaffoldState = rememberBottomSheetScaffoldState()

                val bottomColor = Color(ColorUtils.blendARGB(animatedBackground.toArgb(), MaterialTheme.colors.surface.toArgb(), .15f))

                val uiController = rememberSystemUiController()
                uiController.setStatusBarColor(animatedBackground)
                uiController.setNavigationBarColor(bottomColor)
                //uiController.setSystemBarsColor(animatedBackground)

                var showHistoryPopup by remember { mutableStateOf(false) }

                if (showColorPickerPopup) {
                    ColorPickerView(
                        onColorChange = { hexColor = Integer.toHexString(it.toArgb()).drop(2) }
                    ) { showColorPickerPopup = false }
                }

                var showPopup by remember { mutableStateOf(false) }
                var deleteColor by remember { mutableStateOf<ColorItem?>(null) }

                if (showPopup && deleteColor != null) {

                    val onDismiss = { showPopup = false }

                    val c = Color("#${deleteColor!!.color}".toColorInt())
                    val font = if (c.luminance() > .5f) Color.Black else Color.White

                    AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text("Remove #${deleteColor!!.color}", color = font) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    scope.launch(Dispatchers.IO) { dao.deleteColor(deleteColor!!) }
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

                Surface(color = animatedBackground) {
                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        drawerContent = {
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
                                                    deleteColor = it
                                                    showPopup = true
                                                },
                                                onClick = { hexColor = it.color },
                                                onDoubleClick = {
                                                    hexColor = it.color
                                                    deleteColor = it
                                                    showPopup = true
                                                }
                                            )
                                            .background(c)
                                    )
                                }
                            }
                        },
                        sheetContent = {

                            val infoFontSize = 30.sp

                            @Composable
                            fun InfoText(text: String) {
                                Text(
                                    text,
                                    fontSize = infoFontSize,
                                    textAlign = TextAlign.Center,
                                    color = fontColor,
                                    modifier = Modifier.padding(start = 5.dp)
                                )
                            }

                            Divider(color = fontColor.copy(alpha = .12f))

                            if (showHistoryPopup) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    items(historyColors.toList()) {
                                        val c = Color("#$it".toColorInt())
                                        Card(
                                            onClick = { hexColor = it },
                                            backgroundColor = c,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(CornerSize(0.dp))
                                        ) {
                                            Text(
                                                "#$it",
                                                fontSize = 45.sp,
                                                textAlign = TextAlign.Center,
                                                color = if (c.luminance() > .5f) Color.Black else Color.White,
                                            )
                                        }
                                    }
                                }
                            } else {

                                colorApi?.name?.value?.let { InfoText(it) }

                                val r = (backgroundColor.red * 255).toInt().animateValue()
                                val g = (backgroundColor.green * 255).toInt().animateValue()
                                val b = (backgroundColor.blue * 255).toInt().animateValue()

                                InfoText("RGB: ($r, $g, $b)")

                                colorApi?.let { api ->

                                    api.cmyk?.let { printer ->

                                        val c = printer.c?.animateIntValue()
                                        val m = printer.m?.animateIntValue()
                                        val y = printer.y?.animateIntValue()
                                        val k = printer.k?.animateIntValue()
                                        InfoText("CMYK: ($c, $m, $y, $k)")
                                    }

                                    api.hsl?.let { printer ->
                                        val h = printer.h?.animateIntValue()
                                        val s = printer.s?.animateIntValue()
                                        val l = printer.l?.animateIntValue()
                                        InfoText("HSL: ($h, $s, $l)")
                                    }

                                    api.hsv?.let { printer ->
                                        val h = printer.h?.animateIntValue()
                                        val s = printer.s?.animateIntValue()
                                        val v = printer.v?.animateIntValue()
                                        InfoText("HSV: ($h, $s, $v)")
                                    }

                                    api.XYZ?.let { printer ->
                                        val x = printer.X?.animateIntValue()
                                        val y = printer.Y?.animateIntValue()
                                        val z = printer.Z?.animateIntValue()
                                        InfoText("XYZ: ($x, $y, $z)")
                                    }

                                }

                            }

                            Divider(color = fontColor.copy(alpha = .12f))

                            SettingButton(
                                text = "Show ${if (showHistoryPopup) "Info" else "History"}",
                                fontColor = fontColor,
                                backgroundColor = Color.Transparent
                            ) { showHistoryPopup = !showHistoryPopup }

                            Divider(color = fontColor.copy(alpha = .12f))

                            SettingButton(
                                text = "Color Picker",
                                fontColor = fontColor,
                                backgroundColor = Color.Transparent
                            ) { showColorPickerPopup = true }

                        },
                        sheetBackgroundColor = bottomColor,
                        //drawerBackgroundColor = animatedBackground,
                        sheetElevation = 5.dp,
                        sheetPeekHeight = if (showHistoryPopup) BottomSheetScaffoldDefaults.SheetPeekHeight else 40.dp,
                        backgroundColor = animatedBackground,
                        snackbarHost = {
                            SnackbarHost(it) { data ->
                                Snackbar(
                                    elevation = 15.dp,
                                    backgroundColor = backgroundColor,
                                    contentColor = fontColor,
                                    snackbarData = data
                                )
                            }
                        },
                        topBar = {
                            TopAppBar(
                                title = {

                                    val copyToClipboard: () -> Unit = {
                                        clipboardManager.setText(AnnotatedString("#$hexColor", ParagraphStyle()))
                                        scope.launch { scaffoldState.snackbarHostState.showSnackbar("Copied", null, SnackbarDuration.Short) }
                                    }

                                    Text(
                                        "#$hexColor",
                                        color = fontColor,
                                        fontSize = 45.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onLongClick = copyToClipboard,
                                                onClick = {},
                                                onDoubleClick = copyToClipboard
                                            )
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                if (savedColors.any { it.color == hexColor }) {
                                                    savedColors.find { it.color == hexColor }?.let { dao.deleteColor(it) }
                                                } else {
                                                    dao.insertColor(ColorItem(color = hexColor))
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = fontColor,
                                            modifier = Modifier.rotate(animateFloatAsState(targetValue = if (savedColors.any { it.color == hexColor }) 135f else 0f).value)
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                                                else scaffoldState.bottomSheetState.collapse()
                                            }
                                        }
                                    ) { Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = fontColor) }

                                    IconButton(
                                        onClick = {
                                            hexColor = Integer.toHexString(
                                                Color(
                                                    red = Random.nextInt(0, 255),
                                                    green = Random.nextInt(0, 255),
                                                    blue = Random.nextInt(0, 255)
                                                ).toArgb()
                                            ).drop(2)
                                        }
                                    ) { Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = fontColor) }
                                },
                                backgroundColor = animatedBackground
                            )
                        }
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clipToBounds()
                                .padding(it)
                                .padding(vertical = 5.dp),
                        ) {

                            val m = Modifier.weight(1f / 6f)

                            DigitRow(
                                modifier = m,
                                start = "D",
                                center = "E",
                                end = "F",
                                fontColor = fontColor,
                                onPressStart = onPress,
                                onPressCenter = onPress,
                                onPressEnd = onPress
                            )

                            DigitRow(
                                modifier = m,
                                start = "A",
                                center = "B",
                                end = "C",
                                fontColor = fontColor,
                                onPressStart = onPress,
                                onPressCenter = onPress,
                                onPressEnd = onPress
                            )

                            DigitRow(
                                modifier = m,
                                start = "7",
                                center = "8",
                                end = "9",
                                fontColor = fontColor,
                                onPressStart = onPress,
                                onPressCenter = onPress,
                                onPressEnd = onPress
                            )

                            DigitRow(
                                modifier = m,
                                start = "4",
                                center = "5",
                                end = "6",
                                fontColor = fontColor,
                                onPressStart = onPress,
                                onPressCenter = onPress,
                                onPressEnd = onPress
                            )

                            DigitRow(
                                modifier = m,
                                start = "1",
                                center = "2",
                                end = "3",
                                fontColor = fontColor,
                                onPressStart = onPress,
                                onPressCenter = onPress,
                                onPressEnd = onPress
                            )

                            DigitRow(
                                modifier = m,
                                start = "⊗",
                                center = "0",
                                end = "⌫",
                                fontColor = fontColor,
                                onPressStart = { hexColor = "" },
                                onPressCenter = onPress,
                                onPressEnd = { hexColor = hexColor.dropLast(1) }
                            )
                        }
                    }
                }

                val backCallBack = remember {
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            when {
                                scaffoldState.drawerState.isOpen -> scope.launch { scaffoldState.drawerState.close() }
                                scaffoldState.bottomSheetState.isExpanded -> scope.launch { scaffoldState.bottomSheetState.collapse() }
                                else -> finish()
                            }
                        }
                    }
                }

                DisposableEffect(key1 = onBackPressedDispatcher) {
                    onBackPressedDispatcher.addCallback(backCallBack)
                    onDispose { backCallBack.remove() }
                }

            }
        }
    }
}

@Composable
fun SettingButton(text: String, fontColor: Color, backgroundColor: Color, onClick: () -> Unit = {}) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, fontColor),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = backgroundColor)
    ) {
        Text(
            text,
            color = fontColor,
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@ExperimentalFoundationApi
@Composable
fun DigitItem(modifier: Modifier = Modifier, digit: String, fontColor: Color, onPress: (String) -> Unit) {
    Text(
        digit,
        color = fontColor,
        fontSize = 45.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
            ) { onPress(digit) }
            .then(modifier)
    )
}

@ExperimentalFoundationApi
@Composable
fun DigitRow(
    modifier: Modifier = Modifier,
    start: String,
    center: String,
    end: String,
    fontColor: Color,
    onPressStart: (String) -> Unit,
    onPressCenter: (String) -> Unit,
    onPressEnd: (String) -> Unit
) {
    Row(
        modifier = Modifier.then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val m = Modifier
            .weight(1f)
            .fillMaxHeight(1f)
            .wrapContentHeight(unbounded = true)
            .align(Alignment.CenterVertically)

        DigitItem(m, start, fontColor, onPressStart)
        DigitItem(m, center, fontColor, onPressCenter)
        DigitItem(m, end, fontColor, onPressEnd)
    }
}

@Composable
fun Int.animateValue() = animateIntAsState(targetValue = this).value

@Composable
fun Number.animateIntValue() = toInt().animateValue()