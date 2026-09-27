package com.example.supercalc

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD0BCFF),
                    secondary = Color(0xFFCCC2DC),
                    background = Color(0xFF141218),
                    surface = Color(0xFF2B2930)
                )
            ) {
                CalculatorScreen()
            }
        }
    }
}

@Composable
fun CalculatorScreen() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Вибір зображення з рукописним або друкованим прикладом
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, it)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val line = visionText.text.lines().firstOrNull { l -> l.isNotBlank() } ?: ""
                        val cleaned = line
                            .replace("x", "*")
                            .replace("X", "*")
                            .replace(":", "/")
                            .replace("=", "")
                            .replace("[^0-9\\+\\-\\*/\\(\\)\\.]".toRegex(), "")
                        
                        expression = cleaned
                        if (cleaned.isNotEmpty()) {
                            result = MathEngine.eval(cleaned)
                        } else {
                            Toast.makeText(context, "Приклад не розпізнано", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Помилка OCR: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Помилка відкриття файлу", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Дисплей калькулятора
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                fontSize = 38.sp,
                color = Color.White,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = result,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Кнопки перемикання режимів + OCR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Базові") }
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                label = { Text("Унікальні ⚡") }
            )
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("📷 Сканувати", fontSize = 12.sp, color = Color.Black)
            }
        }

        // Клавіатура
        if (activeTab == 0) {
            StandardKeypad(
                onValueChange = { expression += it },
                onClear = { expression = ""; result = "" },
                onDelete = { if (expression.isNotEmpty()) expression = expression.dropLast(1) },
                onEvaluate = { result = MathEngine.eval(expression) }
            )
        } else {
            UniqueKeypad(
                onFunctionAppend = { func -> expression += func },
                onClear = { expression = ""; result = "" },
                onEvaluate = { result = MathEngine.eval(expression) }
            )
        }
    }
}

@Composable
fun StandardKeypad(
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onEvaluate: () -> Unit
) {
    val buttons = listOf(
        listOf("C", "(", ")", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("⌫", "0", ".", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { btn ->
                    CalcButton(
                        text = btn,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (btn) {
                                "C" -> onClear()
                                "⌫" -> onDelete()
                                "=" -> onEvaluate()
                                else -> onValueChange(btn)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UniqueKeypad(
    onFunctionAppend: (String) -> Unit,
    onClear: () -> Unit,
    onEvaluate: () -> Unit
) {
    val uniqueFunctions = listOf(
        "collatz(" to "Коллатц (Кроки до 1)",
        "digroot(" to "Цифровий Корінь",
        "isprime(" to "Перевірка на Просте (1/0)",
        "fib(" to "N-не число Фібоначчі",
        "superfact(" to "Суперфакторіал",
        "sqrt(" to "Квадратний корінь"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uniqueFunctions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (code, label) ->
                    Button(
                        onClick = { onFunctionAppend(code) },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(label, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Очистити")
            }
            Button(
                onClick = onEvaluate,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Обчислити =")
            }
        }
    }
}

@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isOp = text in listOf("÷", "×", "-", "+", "=", "C", "⌫")
    val containerColor = when (text) {
        "=" -> MaterialTheme.colorScheme.primary
        "C", "⌫" -> Color(0xFF3B2523)
        else -> if (isOp) Color(0xFF36343B) else Color(0xFF2B2930)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (text == "=") Color.Black else Color.White
        )
    }
}
