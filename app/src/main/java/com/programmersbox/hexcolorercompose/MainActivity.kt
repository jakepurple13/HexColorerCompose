package com.programmersbox.hexcolorercompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.edit
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.hexcolorercompose.ui.theme.HexColorerComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = ColorDatabase.getInstance(this).colorDao()

        val dataStore = dataStore

        setContent {
            HexColorerComposeTheme {

                var backgroundColor by remember { mutableStateOf(Color.Black) }

                var hexColor by remember { mutableStateOf("") }

                var colorApi by remember { mutableStateOf<ColorApi?>(null) }

                val historyColors by dataStore.data
                    .map { it[COLOR_HISTORY] ?: emptySet() }
                    .collectAsState(initial = emptySet())

                val scope = rememberCoroutineScope()

                LaunchedEffect(hexColor) {
                    backgroundColor = when (hexColor.length) {
                        6 -> Color("#$hexColor".toColorInt())
                        else -> Color.Black
                    }

                    if (hexColor.length == 6) {
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

                val savedColors by dao
                    .getAllColors()
                    .collectAsState(initial = emptyList())

                val scaffoldState = rememberBottomSheetScaffoldState()

                val uiController = rememberSystemUiController()
                uiController.setStatusBarColor(animatedBackground, backgroundColor.luminance() > .5f)
                uiController.setNavigationBarColor(animatedBackground, backgroundColor.luminance() > .5f)
                uiController.setSystemBarsColor(animatedBackground, backgroundColor.luminance() > .5f)

                var showHistoryPopup by remember { mutableStateOf(false) }

                Surface(color = animatedBackground) {

                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        drawerContent = {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        backgroundColor = animatedBackground
                                    ) {
                                        Text(
                                            "Saved Colors: ${savedColors.size}",
                                            fontSize = 45.sp,
                                            textAlign = TextAlign.Center,
                                            color = fontColor,
                                        )
                                    }
                                },
                                backgroundColor = animatedBackground
                            ) { p ->
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    contentPadding = p
                                ) {
                                    items(savedColors) {
                                        val c = Color("#${it.color}".toColorInt())

                                        Card(
                                            onClick = { hexColor = it.color },
                                            backgroundColor = c,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "#${it.color}",
                                                fontSize = 45.sp,
                                                textAlign = TextAlign.Center,
                                                color = if (c.luminance() > .5f) Color.Black else Color.White,
                                            )
                                        }
                                    }
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
                                            modifier = Modifier.fillMaxWidth()
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
                                backgroundColor = animatedBackground
                            ) { showHistoryPopup = !showHistoryPopup }

                        },
                        sheetBackgroundColor = animatedBackground,
                        drawerBackgroundColor = animatedBackground,
                        sheetElevation = 5.dp,
                        sheetPeekHeight = 0.dp,
                        backgroundColor = animatedBackground,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        "#$hexColor",
                                        color = fontColor,
                                        fontSize = 45.sp,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                if (savedColors.any { it.color == hexColor }) {
                                                    val item = savedColors.find { it.color == hexColor }
                                                    item?.let { dao.deleteColor(it) }
                                                } else {
                                                    dao.insertColor(ColorItem(color = hexColor))
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (savedColors.any { it.color == hexColor }) Icons.Default.Close else Icons.Default.Add,
                                            contentDescription = null,
                                            tint = fontColor
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
                                    ) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = fontColor)
                                    }

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
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = fontColor)
                                    }
                                },
                                backgroundColor = animatedBackground
                            )
                        }
                    ) {

                        ConstraintLayout(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(it)
                                .padding(vertical = 5.dp)
                        ) {

                            val (def, abc, sen, ffs, ott, czb) = createRefs()

                            Row(
                                modifier = Modifier.constrainAs(def) {
                                    top.linkTo(parent.top)
                                }
                            ) {
                                DigitItem("D", fontColor, onPress)
                                DigitItem("E", fontColor, onPress)
                                DigitItem("F", fontColor, onPress)
                            }

                            Row(
                                modifier = Modifier.constrainAs(abc) {
                                    top.linkTo(def.bottom)
                                    bottom.linkTo(sen.top)
                                }
                            ) {
                                DigitItem("A", fontColor, onPress)
                                DigitItem("B", fontColor, onPress)
                                DigitItem("C", fontColor, onPress)
                            }

                            Row(
                                modifier = Modifier.constrainAs(sen) {
                                    top.linkTo(abc.bottom)
                                    bottom.linkTo(ffs.top)
                                }
                            ) {
                                DigitItem("7", fontColor, onPress)
                                DigitItem("8", fontColor, onPress)
                                DigitItem("9", fontColor, onPress)
                            }

                            Row(
                                modifier = Modifier.constrainAs(ffs) {
                                    top.linkTo(sen.bottom)
                                    bottom.linkTo(ott.top)
                                }
                            ) {
                                DigitItem("4", fontColor, onPress)
                                DigitItem("5", fontColor, onPress)
                                DigitItem("6", fontColor, onPress)
                            }

                            Row(
                                modifier = Modifier.constrainAs(ott) {
                                    top.linkTo(ffs.bottom)
                                    bottom.linkTo(czb.top)
                                }
                            ) {
                                DigitItem("1", fontColor, onPress)
                                DigitItem("2", fontColor, onPress)
                                DigitItem("3", fontColor, onPress)
                            }

                            Row(
                                modifier = Modifier.constrainAs(czb) {
                                    bottom.linkTo(parent.bottom)
                                }
                            ) {
                                DigitItem("⊗", fontColor) { hexColor = "" }
                                DigitItem("0", fontColor, onPress)
                                DigitItem("⌫", fontColor) { hexColor = hexColor.dropLast(1) }
                            }

                        }

                    }

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
            fontSize = 45.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@ExperimentalFoundationApi
@Composable
fun RowScope.DigitItem(digit: String, fontColor: Color, onPress: (String) -> Unit) {
    Text(
        digit,
        color = fontColor,
        fontSize = 45.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onPress(digit) }
            .weight(1f)
    )
}

@Composable
fun Int.animateValue() = animateIntAsState(targetValue = this).value

@Composable
fun Number.animateIntValue() = toInt().animateValue()