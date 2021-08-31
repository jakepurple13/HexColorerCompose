package com.programmersbox.hexcolorercompose

import android.content.Context
import android.graphics.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.palette.graphics.Palette
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.programmersbox.gsonutils.getJsonApi
import java.io.ByteArrayOutputStream

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val COLOR_HISTORY = stringSetPreferencesKey("color_history")

val USE_3D = booleanPreferencesKey("use_3d")
val TOPBAR_3D = booleanPreferencesKey("topbar_3d")
val KEYPAD_3D = booleanPreferencesKey("keypad_3d")
val SHEET_3D = booleanPreferencesKey("sheet_3d")
val DRAWER_3D = booleanPreferencesKey("drawer_3d")
val SETTINGS_3D = booleanPreferencesKey("settings_3d")

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
