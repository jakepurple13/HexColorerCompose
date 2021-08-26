package com.programmersbox.hexcolorercompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.edit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.hexcolorercompose.ui.theme.HexColorerComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
                    android.graphics.Color.colorToHSV("#${c.color}".toColorInt(), hsv)
                    hsv[0]
                }
            }

        val use3d = dataStore.data.map { it[USE_3D] ?: false }

        setContent {

            HexColorerComposeTheme {

                val clipboardManager = LocalClipboardManager.current

                var backgroundColor by remember { mutableStateOf(Color.Black) }

                val model = remember { MainModel() }

                val historyColors by history.collectAsState(initial = emptySet())

                val scope = rememberCoroutineScope()

                LaunchedEffect(model.hexColor, model.showColorPickerPopup) {
                    backgroundColor = when (model.hexColor.length) {
                        6 -> Color("#${model.hexColor}".toColorInt())
                        else -> Color.Black
                    }

                    if (model.hexColor.length == 6 && !model.showColorPickerPopup) {
                        scope.launch(Dispatchers.IO) {
                            if (model.hexColor !in historyColors) {
                                dataStore.edit {
                                    it[COLOR_HISTORY] = historyColors.toMutableList()
                                        .apply {
                                            add(0, model.hexColor)
                                            while (size > 5 && isNotEmpty()) removeLast()
                                        }
                                        .toSet()
                                }
                            }
                            model.colorApi = getColorApi(model.hexColor)
                        }
                    }
                }

                val onPress: (String) -> Unit = { if (model.hexColor.length < 6) model.hexColor += it }

                val fontColor = animateColorAsState(
                    if (backgroundColor.luminance() > .5f) Color.Black else Color.White,
                    animationSpec = tween(500)
                ).value

                val settingsDialogModel = remember {
                    SettingsDialogModel(
                        showSettings = false,
                        use3d = runBlocking { use3d.first() }
                    )
                }

                SettingsDialog(
                    settingsDialogModel = settingsDialogModel,
                    dataStore = dataStore,
                    backgroundColor = backgroundColor,
                    fontColor = fontColor,
                    scope = scope
                )

                val animatedBackground = animateColorAsState(backgroundColor, animationSpec = tween(500)).value

                val savedColors by favorites.collectAsState(initial = emptyList())

                val scaffoldState = rememberBottomSheetScaffoldState()

                val bottomColor = Color(ColorUtils.blendARGB(animatedBackground.toArgb(), MaterialTheme.colors.surface.toArgb(), .15f))

                val uiController = rememberSystemUiController()
                uiController.setStatusBarColor(animatedBackground)
                uiController.setNavigationBarColor(bottomColor)

                fun showSnackBar(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
                    scope.launch {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                        scaffoldState.snackbarHostState.showSnackbar(text, null, duration)
                    }
                }

                Surface(color = animatedBackground) {
                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        drawerContent = {
                            Drawer(
                                savedColors = savedColors,
                                animatedBackground = animatedBackground,
                                fontColor = fontColor,
                                model = model,
                                scope = scope,
                                dao = dao
                            )
                        },
                        sheetContent = {
                            Sheet(
                                model = model,
                                historyColors = historyColors,
                                fontColor = fontColor,
                                backgroundColor = backgroundColor
                            )
                        },
                        sheetBackgroundColor = bottomColor,
                        //drawerBackgroundColor = animatedBackground,
                        sheetElevation = 5.dp,
                        sheetPeekHeight = 40.dp,
                        backgroundColor = animatedBackground,
                        snackbarHost = {
                            SnackbarHost(it) { data ->
                                Snackbar(
                                    elevation = 15.dp,
                                    backgroundColor = bottomColor,
                                    contentColor = fontColor,
                                    snackbarData = data
                                )
                            }
                        },
                        topBar = {
                            Header(
                                mainModel = model,
                                settingsDialogModel = settingsDialogModel,
                                fontColor = fontColor,
                                animatedBackground = animatedBackground,
                                clipboardManager = clipboardManager,
                                scope = scope,
                                savedColors = savedColors,
                                scaffoldState = scaffoldState,
                                dao = dao,
                                showSnackBar = ::showSnackBar
                            )
                        }
                    ) {
                        Keypad(
                            paddingValues = it,
                            settingsDialogModel = settingsDialogModel,
                            fontColor = fontColor,
                            model = model,
                            onPress = onPress
                        )
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

class MainModel {
    var hexColor by mutableStateOf("")

    var showColorPickerPopup by mutableStateOf(false)
    var showHistoryPopup by mutableStateOf(false)
    var showDeletePopup by mutableStateOf(false)
    var deleteColor by mutableStateOf<ColorItem?>(null)

    var colorApi by mutableStateOf<ColorApi?>(null)
}
