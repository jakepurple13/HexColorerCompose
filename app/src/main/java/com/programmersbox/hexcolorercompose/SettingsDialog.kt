package com.programmersbox.hexcolorercompose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsDialogModel(
    showSettings: Boolean,
    use3d: Boolean
) {
    var showSettings by mutableStateOf(showSettings)
    var use3d by mutableStateOf(use3d)
}

@Composable
fun SettingsDialog(
    settingsDialogModel: SettingsDialogModel,
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
                LazyColumn {

                    item {

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use 3D Digits", color = fontColor, modifier = Modifier.align(Alignment.CenterStart))
                            Switch(
                                checked = settingsDialogModel.use3d,
                                onCheckedChange = {
                                    settingsDialogModel.use3d = it
                                    scope.launch { dataStore.edit { s -> s[USE_3D] = it } }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = fontColor,
                                    checkedTrackColor = fontColor
                                ),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }

                    }

                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { settingsDialogModel.showSettings = false },
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = backgroundColor),
                    border = BorderStroke(ButtonDefaults.OutlinedBorderSize, fontColor)
                ) {
                    Text("Done", color = fontColor, style = MaterialTheme.typography.button)
                }
            },
            backgroundColor = backgroundColor,
        )

    }

}