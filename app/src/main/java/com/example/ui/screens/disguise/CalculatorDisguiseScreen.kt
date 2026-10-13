package com.example.ui.screens.disguise

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.SecurityManager
import com.example.security.UnlockResult
import com.example.ui.theme.IndigoPrimary
import java.text.DecimalFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorDisguiseScreen(
    securityManager: SecurityManager,
    onUnlocked: (isDecoy: Boolean) -> Unit,
    onPanicTriggered: () -> Unit,
    onOpenPinScreenDirectly: () -> Unit
) {
    var displayValue by remember { mutableStateOf("0") }
    var expressionHistory by remember { mutableStateOf("") }
    var previousValue by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }
    var isNewNumber by remember { mutableStateOf(true) }

    fun checkSecretPin(pinCandidate: String): Boolean {
        return when (securityManager.verifyInput(pinCandidate)) {
            UnlockResult.SUCCESS_MASTER -> {
                onUnlocked(false)
                true
            }
            UnlockResult.SUCCESS_DECOY -> {
                onUnlocked(true)
                true
            }
            UnlockResult.TRIGGER_PANIC -> {
                onPanicTriggered()
                true
            }
            UnlockResult.FAILED -> false
        }
    }

    fun onNumber(num: String) {
        if (isNewNumber || displayValue == "0") {
            displayValue = num
            isNewNumber = false
        } else {
            if (displayValue.length < 12) {
                displayValue += num
            }
        }
    }

    fun onDecimal() {
        if (isNewNumber) {
            displayValue = "0."
            isNewNumber = false
        } else if (!displayValue.contains(".")) {
            displayValue += "."
        }
    }

    fun onClear() {
        displayValue = "0"
        expressionHistory = ""
        previousValue = null
        pendingOperator = null
        isNewNumber = true
    }

    fun onOperator(op: String) {
        val current = displayValue.toDoubleOrNull() ?: 0.0
        if (previousValue == null) {
            previousValue = current
        } else if (pendingOperator != null && !isNewNumber) {
            previousValue = calculate(previousValue!!, current, pendingOperator!!)
            displayValue = formatNumber(previousValue!!)
        }
        pendingOperator = op
        expressionHistory = "${formatNumber(previousValue!!)} $op"
        isNewNumber = true
    }

    fun onEquals() {
        // First check secret PIN entry!
        val cleanInput = displayValue.filter { it.isDigit() }
        if (cleanInput.length in 4..6 && checkSecretPin(cleanInput)) {
            return
        }

        // Otherwise execute standard arithmetic calculation
        val current = displayValue.toDoubleOrNull()
        if (previousValue != null && pendingOperator != null && current != null) {
            val result = calculate(previousValue!!, current, pendingOperator!!)
            expressionHistory = "${formatNumber(previousValue!!)} $pendingOperator ${formatNumber(current)} ="
            displayValue = formatNumber(result)
            previousValue = null
            pendingOperator = null
            isNewNumber = true
        }
    }

    fun onToggleSign() {
        val current = displayValue.toDoubleOrNull() ?: return
        displayValue = formatNumber(-current)
    }

    fun onPercent() {
        val current = displayValue.toDoubleOrNull() ?: return
        displayValue = formatNumber(current / 100.0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121418))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Calculator Display Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onOpenPinScreenDirectly
                )
                .padding(horizontal = 12.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expressionHistory,
                color = Color(0xFF717D96),
                fontSize = 20.sp,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayValue,
                color = Color.White,
                fontSize = if (displayValue.length > 8) 40.sp else 54.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.testTag("calculator_display")
            )
        }

        // Calculator Keypad
        val buttonRows = listOf(
            listOf("AC" to Color(0xFF4A5568), "+/-" to Color(0xFF4A5568), "%" to Color(0xFF4A5568), "÷" to IndigoPrimary),
            listOf("7" to Color(0xFF252A34), "8" to Color(0xFF252A34), "9" to Color(0xFF252A34), "×" to IndigoPrimary),
            listOf("4" to Color(0xFF252A34), "5" to Color(0xFF252A34), "6" to Color(0xFF252A34), "-" to IndigoPrimary),
            listOf("1" to Color(0xFF252A34), "2" to Color(0xFF252A34), "3" to Color(0xFF252A34), "+" to IndigoPrimary),
            listOf("0" to Color(0xFF252A34), "." to Color(0xFF252A34), "=" to IndigoPrimary)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (label, bg) ->
                        val isZero = label == "0"
                        val weight = if (isZero) 2f else 1f

                        Surface(
                            modifier = Modifier
                                .weight(weight)
                                .aspectRatio(if (isZero) 2.1f else 1f)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        when (label) {
                                            "AC" -> onClear()
                                            "+/-" -> onToggleSign()
                                            "%" -> onPercent()
                                            "÷" -> onOperator("/")
                                            "×" -> onOperator("*")
                                            "-" -> onOperator("-")
                                            "+" -> onOperator("+")
                                            "=" -> onEquals()
                                            "." -> onDecimal()
                                            else -> onNumber(label)
                                        }
                                    },
                                    onLongClick = {
                                        if (label == "=") {
                                            onEquals()
                                        }
                                    }
                                ),
                            color = bg,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculate(a: Double, b: Double, op: String): Double {
    return when (op) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b != 0.0) a / b else 0.0
        else -> b
    }
}

private fun formatNumber(num: Double): String {
    return if (num == num.toLong().toDouble()) {
        num.toLong().toString()
    } else {
        DecimalFormat("#.######").format(num)
    }
}
