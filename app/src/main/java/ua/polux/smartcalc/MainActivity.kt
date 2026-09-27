package ua.polux.smartcalc

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartCalcApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCalcApp() {
    val context = LocalContext.current
    val engine = remember { MathEngine() }
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var angleMode by remember { mutableStateOf(MathEngine.AngleMode.DEG) }
    var ocrBusy by remember { mutableStateOf(false) }
    var showFunctions by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun calculate() {
        if (expression.isBlank()) return
        try {
            result = engine.solve(expression, angleMode)
        } catch (e: Exception) {
            result = "Помилка"
            Toast.makeText(context, e.message ?: "Невірний вираз", Toast.LENGTH_SHORT).show()
        }
    }

    fun processImage(uri: Uri) {
        ocrBusy = true
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { vision ->
                    val cleaned = OcrMath.clean(vision.text)
                    if (cleaned.isBlank()) {
                        Toast.makeText(context, "Не вдалося знайти приклад", Toast.LENGTH_LONG).show()
                    } else {
                        expression = cleaned
                        try {
                            result = engine.solve(cleaned, angleMode)
                        } catch (_: Exception) {
                            result = "?"
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "OCR: не вдалося розпізнати фото", Toast.LENGTH_LONG).show()
                }
                .addOnCompleteListener {
                    ocrBusy = false
                    recognizer.close()
                }
        } catch (e: Exception) {
            ocrBusy = false
            recognizer.close()
            Toast.makeText(context, "Не вдалося відкрити зображення", Toast.LENGTH_SHORT).show()
        }
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(::processImage) }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) pendingCameraUri?.let(::processImage)
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File.createTempFile("math_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            pendingCameraUri = uri
            camera.launch(uri)
        } else {
            Toast.makeText(context, "Потрібен доступ до камери", Toast.LENGTH_SHORT).show()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0B0D10),
            surface = Color(0xFF15181D),
            surfaceVariant = Color(0xFF20252D),
            primary = Color(0xFF9CCAFF),
            onPrimary = Color(0xFF003258),
            onBackground = Color(0xFFE4E6EB)
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("SmartCalc", fontWeight = FontWeight.Bold)
                            Text("математика без меж", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        TextButton(onClick = { angleMode = if (angleMode == MathEngine.AngleMode.DEG) MathEngine.AngleMode.RAD else MathEngine.AngleMode.DEG }) {
                            Text(if (angleMode == MathEngine.AngleMode.DEG) "DEG" else "RAD")
                        }
                    }
                )
            }
        ) { pad ->
            Column(
                modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(0.26f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            expression.ifBlank { "Введи приклад або сфотографуй його" },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            fontSize = 26.sp,
                            maxLines = 3
                        )
                        Text(
                            result,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Default.CameraAlt, "Фото") {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val dir = File(context.cacheDir, "images").apply { mkdirs() }
                            val file = File.createTempFile("math_", ".jpg", dir)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            pendingCameraUri = uri
                            camera.launch(uri)
                        } else permission.launch(Manifest.permission.CAMERA)
                    }
                    ActionButton(Icons.Default.PhotoLibrary, "Галерея") { gallery.launch("image/*") }
                    ActionButton(Icons.Default.Delete, "Очистити") { expression = ""; result = "0" }
                }

                if (ocrBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showFunctions = !showFunctions }) {
                        Text(if (showFunctions) "Основна клавіатура" else "Функції ∞")
                    }
                    Text("×, ÷, √, ^, !, nCr, gcd, fib…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val keys = if (showFunctions) functionKeys else basicKeys
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(0.66f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(keys) { key ->
                        CalcKey(key) {
                            when (key) {
                                "=" -> calculate()
                                "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
                                "AC" -> { expression = ""; result = "0" }
                                else -> {
                                    expression += key
                                    if (key == "ans") result = result
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun androidx.compose.foundation.layout.RowScope.ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        Icon(icon, null)
        Spacer(Modifier.width(4.dp))
        Text(text)
    }
}

@Composable
fun CalcKey(label: String, onClick: () -> Unit) {
    val primary = label in setOf("=", "+", "-", "×", "÷", "^")
    val special = label in setOf("AC", "⌫")
    Surface(
        modifier = Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = when {
            primary -> MaterialTheme.colorScheme.primary
            special -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = if (label.length > 5) 15.sp else 21.sp,
                fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
                color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private val basicKeys = listOf(
    "AC", "⌫", "(", ")",
    "7", "8", "9", "÷",
    "4", "5", "6", "×",
    "1", "2", "3", "-",
    "0", ".", "%", "+",
    "pi", "e", "^", "="
)

private val functionKeys = listOf(
    "sqrt(", "cbrt(", "abs(", "!",
    "sin(", "cos(", "tan(", "asin(",
    "acos(", "atan(", "ln(", "log10(",
    "log(", "root(", "pow(", "hypot(",
    "gcd(", "lcm(", "ncr(", "npr(",
    "fib(", "isprime(", "floor(", "ceil(",
    "round(", "frac(", "exp(", "sigmoid(",
    "(", ")", ",", "="
)
