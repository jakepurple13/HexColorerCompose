package com.programmersbox.hexcolorercompose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.graphics.toColorInt
import java.util.*

@Composable
fun Int.animateValue() = animateIntAsState(targetValue = this).value

@Composable
fun Number.animateIntValue() = toInt().animateValue()

@ExperimentalMaterialApi
@Composable
fun Sheet(
    model: MainModel,
    historyColors: Set<String>,
    fontColor: Color,
    backgroundColor: Color,
) {

    if (model.showColorPickerPopup) {
        ColorPickerView(
            currentColor = backgroundColor,
            onColorChange = { model.hexColor = Integer.toHexString(it.toArgb()).drop(2) }
        ) { model.showColorPickerPopup = false }
    }

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

    model.colorApi?.name?.value?.let { InfoText(it) }

    val r = (backgroundColor.red * 255).toInt().animateValue()
    val g = (backgroundColor.green * 255).toInt().animateValue()
    val b = (backgroundColor.blue * 255).toInt().animateValue()
    InfoText("RGB: ($r, $g, $b)")

    model.colorApi?.let { api ->

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

    Divider(color = fontColor.copy(alpha = .12f))

    SettingButton(
        text = "Color Picker",
        fontColor = fontColor,
        backgroundColor = Color.Transparent
    ) { model.showColorPickerPopup = true }

    Divider(color = fontColor.copy(alpha = .12f))

    SettingButton(
        text = "${if (model.showHistoryPopup) "Hide" else "Show"} History",
        fontColor = fontColor,
        backgroundColor = Color.Transparent
    ) { model.showHistoryPopup = !model.showHistoryPopup }

    if (model.showHistoryPopup) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(historyColors.toList()) {
                val c = Color("#$it".toColorInt())
                Card(
                    onClick = { model.hexColor = it },
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
    }

}

@Composable
fun ColorPickerView(currentColor: Color, onColorChange: (Color) -> Unit, onDismiss: () -> Unit) {

    var color by remember { mutableStateOf(currentColor) }
    val contentColor = if (color.luminance() > .5f) Color.Black else Color.White
    val text = "#" + Integer.toHexString(color.toArgb()).uppercase(Locale.ROOT).drop(2)

    AlertDialog(
        onDismissRequest = {
            onColorChange(color)
            onDismiss()
        },
        text = { ColorPicker { color = it } },
        title = {
            Text(
                "Color Picker: $text",
                style = MaterialTheme.typography.h6,
                color = contentColor
            )
        },
        buttons = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.padding(8.dp),
            ) {
                LabelSlider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    label = "R",
                    fontColor = contentColor,
                    value = color.red * 255,
                    sliderColor = Color.Red
                ) { color = color.copy(red = it / 255f) }

                LabelSlider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    label = "G",
                    fontColor = contentColor,
                    value = color.green * 255,
                    sliderColor = Color.Green
                ) { color = color.copy(green = it / 255f) }

                LabelSlider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    label = "B",
                    fontColor = contentColor,
                    value = color.blue * 255,
                    sliderColor = Color.Blue
                ) { color = color.copy(blue = it / 255f) }

                OutlinedButton(
                    onClick = {
                        onColorChange(color)
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                    border = BorderStroke(ButtonDefaults.OutlinedBorderSize, contentColor),
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Done", color = contentColor, style = MaterialTheme.typography.button) }
            }
        },
        backgroundColor = color
    )

}

@Composable
private fun LabelSlider(
    modifier: Modifier = Modifier,
    label: String,
    fontColor: Color = MaterialTheme.colors.onBackground,
    value: Float,
    sliderColor: Color,
    onSliderChange: (Float) -> Unit
) {
    BoxWithConstraints {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.h6,
                fontSize = 16.sp,
                color = fontColor,
                modifier = Modifier.width(10.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Slider(
                value = value,
                onValueChange = onSliderChange,
                valueRange = 0f..255f,
                steps = 255,
                modifier = Modifier
                    .width(this@BoxWithConstraints.maxWidth - 56.dp),
                colors = SliderDefaults.colors(
                    activeTickColor = Color.Unspecified,
                    activeTrackColor = sliderColor,
                    thumbColor = sliderColor,
                    inactiveTickColor = Color.Unspecified
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                Modifier
                    .width(40.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    value.toInt().toString(),
                    color = fontColor,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}

@Composable
fun ColorPicker(modifier: Modifier = Modifier, onColorChange: (Color) -> Unit) {
    BoxWithConstraints(
        Modifier
            .padding(50.dp)
            .wrapContentSize()
            .then(modifier)
    ) {
        val diameter = constraints.maxWidth
        var position by remember { mutableStateOf(Offset.Zero) }
        val colorWheel = remember(diameter) { ColorWheel(diameter) }

        var hasInput by remember { mutableStateOf(false) }
        val inputModifier = Modifier.pointerInput(colorWheel) {
            fun updateColorWheel(newPosition: Offset) {
                // Work out if the new position is inside the circle we are drawing, and has a
                // valid color associated to it. If not, keep the current position
                val newColor = colorWheel.colorForPosition(newPosition)
                if (newColor.isSpecified) {
                    position = newPosition
                    onColorChange(newColor)
                }
            }

            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown()
                    hasInput = true
                    updateColorWheel(down.position)
                    drag(down.id) { change ->
                        change.consumePositionChange()
                        updateColorWheel(change.position)
                    }
                    hasInput = false
                }
            }
        }

        Box(Modifier.wrapContentSize()) {
            val color = colorWheel.colorForPosition(position)
            Image(
                modifier = inputModifier.border(2.dp, if (color.luminance() > .5f) Color.Black else Color.White, CircleShape),
                contentDescription = null,
                bitmap = colorWheel.image
            )
            if (color.isSpecified) {
                Magnifier(visible = hasInput, position = position, color = color)
            }
        }
    }
}

/**
 * Magnifier displayed on top of [position] with the currently selected [color].
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Magnifier(visible: Boolean, position: Offset, color: Color) {
    val offset = with(LocalDensity.current) {
        Modifier.offset(
            position.x.toDp() - MagnifierWidth / 2,
            // Align with the center of the selection circle
            position.y.toDp() - (MagnifierHeight - (SelectionCircleDiameter / 2))
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        MagnifierTransition(
            visible,
            MagnifierWidth,
            SelectionCircleDiameter
        ) { labelWidth: Dp, selectionDiameter: Dp, alpha: Float ->
            Column(
                offset
                    .size(width = MagnifierWidth, height = MagnifierHeight)
                    .alpha(alpha)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MagnifierLabel(Modifier.size(labelWidth, MagnifierLabelHeight), color)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(SelectionCircleDiameter),
                    contentAlignment = Alignment.Center
                ) { MagnifierSelectionCircle(Modifier.size(selectionDiameter), color) }
            }
        }
    }
}

private val MagnifierWidth = 110.dp
private val MagnifierHeight = 100.dp
private val MagnifierLabelHeight = 50.dp
private val SelectionCircleDiameter = 30.dp

/**
 * [Transition] that animates between [visible] states of the magnifier by animating the width of
 * the label, diameter of the selection circle, and alpha of the overall magnifier
 */
@Composable
private fun MagnifierTransition(
    visible: Boolean,
    maxWidth: Dp,
    maxDiameter: Dp,
    content: @Composable (labelWidth: Dp, selectionDiameter: Dp, alpha: Float) -> Unit
) {
    val transition = updateTransition(visible, label = "")
    val labelWidth by transition.animateDp(transitionSpec = { tween() }, label = "") { if (it) maxWidth else 0.dp }
    val magnifierDiameter by transition.animateDp(transitionSpec = { tween() }, label = "") { if (it) maxDiameter else 0.dp }
    val alpha by transition.animateFloat(transitionSpec = { tween() }, label = "") { if (it) 1f else 0f }
    content(labelWidth, magnifierDiameter, alpha)
}

/**
 * Label representing the currently selected [color], with [Text] representing the hex code and a
 * square at the start showing the [color].
 */
@Composable
private fun MagnifierLabel(modifier: Modifier, color: Color) {
    Popup {
        Surface(shape = MagnifierPopupShape, elevation = 4.dp) {
            Row(modifier) {
                Box(
                    Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .background(color)
                )
                // Add `#` and drop alpha characters
                val text = "#" + Integer.toHexString(color.toArgb()).uppercase(Locale.ROOT).drop(2)
                val textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                Text(
                    text = text,
                    modifier = Modifier
                        .weight(0.75f)
                        .padding(top = 10.dp, bottom = 20.dp),
                    style = textStyle,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Selection circle drawn over the currently selected pixel of the color wheel.
 */
@Composable
private fun MagnifierSelectionCircle(modifier: Modifier, color: Color) {
    Surface(
        modifier,
        shape = CircleShape,
        elevation = 4.dp,
        color = color,
        border = BorderStroke(2.dp, SolidColor(Color.Black.copy(alpha = 0.75f))),
        content = {}
    )
}

/**
 * A [GenericShape] that draws a box with a triangle at the bottom center to indicate a popup.
 */
private val MagnifierPopupShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height

    val arrowY = height * 0.8f
    val arrowXOffset = width * 0.4f

    addRoundRect(RoundRect(0f, 0f, width, arrowY, cornerRadius = CornerRadius(20f, 20f)))

    moveTo(arrowXOffset, arrowY)
    lineTo(width / 2f, height)
    lineTo(width - arrowXOffset, arrowY)
    close()
}

/**
 * A color wheel with an [ImageBitmap] that draws a circular color wheel of the specified diameter.
 */
private class ColorWheel(diameter: Int) {
    private val radius = diameter / 2f

    private val sweepGradient = SweepGradientShader(
        colors = listOf(
            Color.Red,
            Color.Magenta,
            Color.Blue,
            Color.Cyan,
            Color.Green,
            Color.Yellow,
            Color.Red
        ),
        colorStops = null,
        center = Offset(radius, radius)
    )

    val image = ImageBitmap(diameter, diameter).also { imageBitmap ->
        val canvas = Canvas(imageBitmap)
        val center = Offset(radius, radius)
        val paint = Paint().apply { shader = sweepGradient }
        canvas.drawCircle(center, radius, paint)
    }
}

/**
 * @return the matching color for [position] inside [ColorWheel], or `null` if there is no color
 * or the color is partially transparent.
 */
private fun ColorWheel.colorForPosition(position: Offset): Color {
    val x = position.x.toInt().coerceAtLeast(0)
    val y = position.y.toInt().coerceAtLeast(0)
    with(image.toPixelMap()) {
        if (x >= width || y >= height) return Color.Unspecified
        return this[x, y].takeIf { it.alpha == 1f } ?: Color.Unspecified
    }
}