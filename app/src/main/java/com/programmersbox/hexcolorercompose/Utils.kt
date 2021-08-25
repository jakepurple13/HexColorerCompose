package com.programmersbox.hexcolorercompose

import android.content.Context
import android.graphics.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.palette.graphics.Palette
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.programmersbox.gsonutils.getJsonApi
import java.io.ByteArrayOutputStream
import java.util.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val COLOR_HISTORY = stringSetPreferencesKey("color_history")

data class ColorApi(
    val hex: Hex?,
    val rgb: Rgb?,
    val hsl: Hsl?,
    val hsv: Hsv?,
    val name: Name?,
    val cmyk: Cmyk?,
    val XYZ: XYZ?,
    val image: Image?,
    val contrast: Contrast?,
    val _links: _links?,
    val _embedded: _embedded?
)

data class Cmyk(val fraction: Fraction?, val value: String?, val c: Number?, val m: Number?, val y: Number?, val k: Number?)

data class Contrast(val value: String?)

data class Fraction(val c: Number?, val m: Number?, val y: Number?, val k: Number?)

data class Hex(val value: String?, val clean: String?)

data class Hsl(val fraction: Fraction?, val h: Number?, val s: Number?, val l: Number?, val value: String?)

data class Hsv(val fraction: Fraction?, val value: String?, val h: Number?, val s: Number?, val v: Number?)

data class Image(val bare: String?, val named: String?)

data class Name(val value: String?, val closest_named_hex: String?, val exact_match_name: Boolean?, val distance: Number?)

data class Rgb(val fraction: Fraction?, val r: Number?, val g: Number?, val b: Number?, val value: String?)

data class Self(val href: String?)

data class XYZ(val fraction: Fraction?, val value: String?, val X: Number?, val Y: Number?, val Z: Number?)

class _embedded()

data class _links(val self: Self?)

fun getColorApi(color: String) = try {
    getJsonApi<ColorApi>("http://thecolorapi.com/id?hex=$color")
} catch (e: Exception) {
    null
}

@ExperimentalPermissionsApi
@Composable
fun CameraView(dismiss: () -> Unit) {

    var paletteSwatches by remember { mutableStateOf<Palette?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = dismiss,
        text = {

            // Track if the user doesn't want to see the rationale any more.
            var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

            val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
            PermissionRequired(
                permissionState = cameraPermissionState,
                permissionNotGrantedContent = {
                    if (doNotShowRationale) {
                        Text("Feature not available")
                    } else {
                        Column {
                            Text("The camera is important for this app. Please grant the permission.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                                    Text("Ok!")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { doNotShowRationale = true }) {
                                    Text("Nope")
                                }
                            }
                        }
                    }
                },
                permissionNotAvailableContent = {
                    Column {
                        Text(
                            "Camera permission denied. See this FAQ with information about why we " +
                                    "need this permission. Please, grant us access on the Settings screen."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = dismiss) {
                            Text("Close")
                        }
                    }
                }
            ) {
                val lifecycleOwner = LocalLifecycleOwner.current
                val context = LocalContext.current
                val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

                Column {

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            previewView.controller?.isPinchToZoomEnabled = true
                            previewView.controller?.isTapToFocusEnabled = true
                            //previewView.controller?
                            val executor = ContextCompat.getMainExecutor(ctx)
                            cameraProviderFuture.addListener(
                                {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val cameraSelector = CameraSelector.Builder()
                                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                        .build()

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .apply {
                                            setAnalyzer(
                                                executor,
                                                PaletteAnalyzer { paletteSwatches = it.palette }
                                            )
                                        }

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        imageAnalysis,
                                        preview
                                    )
                                },
                                executor
                            )
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (isPaused) {

                        }
                    }

                }
            }

        },
        buttons = {

            /*Button(
                onClick = { showCameraPopup = false },
                colors = ButtonDefaults.buttonColors(backgroundColor = animatedBackground)
            ) {
                Text(
                    "Done",
                    textAlign = TextAlign.Center,
                    color = fontColor,
                )
            }*/

            Column(
                modifier = Modifier.padding(5.dp)
            ) {

                Row {
                    Button(
                        onClick = dismiss,
                    ) {
                        Text(
                            "Done",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.button
                        )
                    }

                    Button(
                        onClick = {
                            //pause here
                            isPaused = !isPaused
                        },
                    ) {
                        Text(
                            "Pause/Start",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.button
                        )
                    }

                }

                LazyRow {

                    items(paletteSwatches?.swatches.orEmpty()) {

                        Column {

                            Text("Rgb", color = Color(it.rgb))
                            Text("Body", color = Color(it.bodyTextColor))
                            Text("Title", color = Color(it.titleTextColor))

                        }

                    }

                }

            }

        }
    )

}

class PaletteAnalyzer(private val onColorChange: (MeshColor) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.convertImageProxyToBitmap().let {
            val palette = Palette.Builder(it).generate()
            onColorChange.invoke(
                MeshColor(
                    palette,
                    imageProxy.imageInfo.timestamp
                )
            )
        }
        imageProxy.close()
    }
}

//based on the code from https://stackoverflow.com/a/56812799
fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}


data class MeshColor(
    val palette: Palette,
    val timestamp: Long
)

@Composable
fun ColorPickerView(onColorChange: (Color) -> Unit, onDismiss: () -> Unit) {

    var color by remember { mutableStateOf(Color.Black) }
    val contentColor = if (color.luminance() > .5f) Color.Black else Color.White
    val text = "#" + Integer.toHexString(color.toArgb()).uppercase(Locale.ROOT).drop(2)

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            ColorPicker(
                onColorChange = {
                    color = it
                    onColorChange(it)
                }
            )
        },
        title = {
            Text(
                "Color Picker: $text",
                style = MaterialTheme.typography.h6
            )
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = color),
                border = BorderStroke(ButtonDefaults.OutlinedBorderSize, contentColor)
            ) {
                Text("Done", color = contentColor, style = MaterialTheme.typography.button)
            }
        },
    )

}

@Composable
fun ColorPicker(onColorChange: (Color) -> Unit) {
    BoxWithConstraints(
        Modifier
            .padding(50.dp)
            .wrapContentSize()
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
            Image(modifier = inputModifier, contentDescription = null, bitmap = colorWheel.image)
            val color = colorWheel.colorForPosition(position)
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