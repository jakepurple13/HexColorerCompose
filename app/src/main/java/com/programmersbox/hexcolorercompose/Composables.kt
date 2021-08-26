package com.programmersbox.hexcolorercompose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun Digit3DItem(modifier: Modifier = Modifier, digit: String, fontColor: Color, onPress: (String) -> Unit) {
    Box(
        Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
            ) { onPress(digit) }
            .then(modifier)
    ) {
        Text(
            digit,
            color = if (fontColor.luminance() > .5f) Color.Black else Color.White,
            fontSize = 45.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .offset(2.dp, 2.dp)
                .align(Alignment.Center)
        )

        Text(
            digit,
            color = fontColor,
            fontSize = 45.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@ExperimentalFoundationApi
@Composable
fun DigitItem(modifier: Modifier = Modifier, digit: String, fontColor: Color, use3d: Boolean, onPress: (String) -> Unit) =
    if (use3d) Digit3DItem(modifier, digit, fontColor, onPress) else DigitItem(modifier, digit, fontColor, onPress)


@ExperimentalFoundationApi
@Composable
fun DigitRow(
    modifier: Modifier = Modifier,
    start: String,
    center: String,
    end: String,
    fontColor: Color,
    use3d: Boolean,
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

        DigitItem(m, start, fontColor, use3d, onPressStart)
        DigitItem(m, center, fontColor, use3d, onPressCenter)
        DigitItem(m, end, fontColor, use3d, onPressEnd)
    }
}

@ExperimentalFoundationApi
@Composable
fun Keypad(
    paddingValues: PaddingValues,
    settingsDialogModel: SettingsDialogModel,
    fontColor: Color,
    model: MainModel,
    onPress: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clipToBounds()
            .padding(paddingValues)
            .padding(vertical = 5.dp),
    ) {

        val m = Modifier.weight(1f / 6f)

        DigitRow(
            modifier = m,
            start = "D",
            center = "E",
            end = "F",
            use3d = settingsDialogModel.use3d,
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
            use3d = settingsDialogModel.use3d,
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
            use3d = settingsDialogModel.use3d,
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
            use3d = settingsDialogModel.use3d,
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
            use3d = settingsDialogModel.use3d,
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
            use3d = settingsDialogModel.use3d,
            fontColor = fontColor,
            onPressStart = { model.hexColor = "" },
            onPressCenter = onPress,
            onPressEnd = { model.hexColor = model.hexColor.dropLast(1) }
        )
    }
}