package com.programmersbox.hexcolorercompose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsDialogModel(
    showSettings: Boolean
) {
    var showSettings by mutableStateOf(showSettings)
}

@Composable
fun SettingsDialog(
    settingsDialogModel: SettingsDialogModel,
    use3d: Boolean, keypad: Boolean, topbar: Boolean, sheet: Boolean, drawer: Boolean,
    dataStore: DataStore<Preferences>,
    backgroundColor: Color,
    fontColor: Color,
    scope: CoroutineScope
) {

    if (settingsDialogModel.showSettings) {

        AlertDialog(
            onDismissRequest = { settingsDialogModel.showSettings = false },
            title = { Text("Settings", style = MaterialTheme.typography.h6, color = fontColor) },
            text = {
                LazyColumn(
                    modifier = Modifier.animateContentSize()
                ) {

                    item {

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use 3D Digits", color = fontColor, modifier = Modifier.align(Alignment.CenterStart))
                            Switch(
                                checked = use3d,
                                onCheckedChange = { scope.launch { dataStore.edit { s -> s[USE_3D] = it } } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = fontColor,
                                    checkedTrackColor = fontColor
                                ),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }

                        val parentState = remember(keypad, topbar) {
                            if (keypad && topbar) ToggleableState.On
                            else if (!keypad && !topbar) ToggleableState.Off
                            else ToggleableState.Indeterminate
                        }

                        Column(
                            modifier = Modifier.padding(start = 5.dp)
                        ) {

                            val checkboxColors = CheckboxDefaults.colors(
                                checkedColor = fontColor,
                                uncheckedColor = fontColor,
                                checkmarkColor = if (fontColor.luminance() > .5f) Color.Black else Color.White
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("3D Options", color = fontColor, modifier = Modifier.align(Alignment.CenterStart))
                                TriStateCheckbox(
                                    state = parentState,
                                    onClick = {
                                        val state = parentState != ToggleableState.On
                                        scope.launch { dataStore.edit { s -> s[KEYPAD_3D] = state } }
                                        scope.launch { dataStore.edit { s -> s[TOPBAR_3D] = state } }
                                        scope.launch { dataStore.edit { s -> s[SHEET_3D] = state } }
                                        scope.launch { dataStore.edit { s -> s[DRAWER_3D] = state } }
                                    },
                                    colors = checkboxColors,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    enabled = use3d
                                )
                            }

                            CheckboxSetting(
                                text = "Keypad",
                                fontColor = fontColor,
                                checkboxColors = checkboxColors,
                                state = keypad,
                                enabled = use3d
                            ) { scope.launch { dataStore.edit { s -> s[KEYPAD_3D] = it } } }

                            CheckboxSetting(
                                text = "Topbar",
                                fontColor = fontColor,
                                checkboxColors = checkboxColors,
                                state = topbar,
                                enabled = use3d
                            ) { scope.launch { dataStore.edit { s -> s[TOPBAR_3D] = it } } }

                            CheckboxSetting(
                                text = "Bottom Sheet",
                                fontColor = fontColor,
                                checkboxColors = checkboxColors,
                                state = sheet,
                                enabled = use3d
                            ) { scope.launch { dataStore.edit { s -> s[SHEET_3D] = it } } }

                            CheckboxSetting(
                                text = "Drawer",
                                fontColor = fontColor,
                                checkboxColors = checkboxColors,
                                state = drawer,
                                enabled = use3d
                            ) { scope.launch { dataStore.edit { s -> s[DRAWER_3D] = it } } }

                        }

                    }

                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { settingsDialogModel.showSettings = false },
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = backgroundColor),
                    border = BorderStroke(ButtonDefaults.OutlinedBorderSize, fontColor)
                ) { Text("Done", color = fontColor, style = MaterialTheme.typography.button) }
            },
            backgroundColor = backgroundColor,
        )

    }

}

@Composable
fun CheckboxSetting(text: String, fontColor: Color, checkboxColors: CheckboxColors, state: Boolean, enabled: Boolean, onCheck: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    ) {
        Text(text, color = fontColor, modifier = Modifier.align(Alignment.CenterStart))
        Checkbox(
            checked = state,
            onCheckedChange = onCheck,
            colors = checkboxColors,
            enabled = enabled,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}