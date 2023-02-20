@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.math.cos
import kotlin.math.sin

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface {
            Column {
                EquationGraph(
                    modifier = Modifier.fillMaxSize(),
                    equations = listOf(
                        EquationGraphLine(equation = { it }, lineColor = Color.Blue, pixelStep = 1f),
                        EquationGraphLine(equation = { it * it }, lineColor = Color.Red, pixelStep = 1f),
                        EquationGraphLine(equation = { sin(it) }, lineColor = Color.Cyan, pixelStep = 1f),
                        EquationGraphLine(equation = { cos(it) }, lineColor = Color.Green, pixelStep = 1f)
                    )
                )
            }

        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

data class EquationGraphLine(
    val equation: Equation,
    val lineColor: Color,
    val pointMode: PointMode = PointMode.Polygon,
    val cap: StrokeCap = StrokeCap.Round,
    val lineThickness: Float = 1f,
    val pixelStep: Float = 0.1f
)

@Composable
fun EquationGraph(
    modifier: Modifier = Modifier,
    equations: List<EquationGraphLine>,
    oneUnitToPixels: Float = 100f
) = Box(modifier = modifier) {
    val regex = Regex("""[\d.]*""")
    var unitToPixels by remember { mutableStateOf(oneUnitToPixels) }
    var textInTextField by remember { mutableStateOf(unitToPixels.toString()) }
    var relativeOffset by remember { mutableStateOf(Offset.Zero) }
    Column {
        EquationGraphCanvas(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .onDrag { relativeOffset = Offset(relativeOffset.x + it.x, relativeOffset.y + it.y) },
            equations = equations,
            oneUnitToPixels = unitToPixels,
            relativeOffset = relativeOffset
        )
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    textInTextField,
                    maxLines = 1,
                    modifier = Modifier.width(128.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    onValueChange = { newValue ->
                        if (regex.matches(newValue)) textInTextField = newValue
                        textInTextField.toFloatOrNull()?.takeUnless { it == 0f }?.let {
                            unitToPixels = it
                        }
                    })
                IconButton(onClick = {
                    unitToPixels /= 2
                    textInTextField = unitToPixels.toString()
                }) {
                    Icon(imageVector = Icons.Filled.ZoomOut, "Zoom Out")
                }
                IconButton(onClick = {
                    unitToPixels *= 2
                    textInTextField = unitToPixels.toString()
                }) {
                    Icon(imageVector = Icons.Filled.ZoomIn, "Zoom In")
                }
            }
        }
    }

}

@Composable
fun EquationGraphCanvas(
    modifier: Modifier = Modifier,
    equations: List<EquationGraphLine>,
    oneUnitToPixels: Float = 100f,
    relativeOffset: Offset = Offset.Zero,
    ratioX: Float = 0.5f,
    ratioY: Float = 0.5f
) = Canvas(
    modifier = modifier
) {
    val centerX = size.width * ratioX
    val centerY = size.height * ratioY
    drawLine(
        start = Offset(centerX + relativeOffset.x, 0f),
        end = Offset(centerX + relativeOffset.x, size.height),
        color = Color.Black,
        strokeWidth = 1f,
        alpha = 0.5f
    )
    drawLine(
        start = Offset(0f, centerY + relativeOffset.y),
        end = Offset(size.width, centerY + relativeOffset.y),
        color = Color.Black,
        strokeWidth = 1f,
        alpha = 0.5f
    )
    equations.forEach {
        drawEquationLine(
            equation = it.equation,
            lineColor = it.lineColor,
            oneUnitToPixelsOfX = oneUnitToPixels,
            oneUnitToPixelsOfY = oneUnitToPixels,
            pointMode = it.pointMode,
            cap = it.cap,
            lineThickness = it.lineThickness,
            pixelStep = it.pixelStep,
            relativeOffsetX = relativeOffset.x + centerX,
            relativeOffsetY = relativeOffset.y + centerY
        )
    }
}


private infix fun ClosedFloatingPointRange<Float>.step(step: Float): List<Float> {
    val list = mutableListOf<Float>()
    var current = start
    while (current < endInclusive) {
        list.add(current)
        current += step
    }
    list.add(endInclusive)
    return list
}

fun DrawScope.drawEquationLine(
    equation: Equation,
    lineColor: Color,
    oneUnitToPixelsOfX: Float = 100f,
    oneUnitToPixelsOfY: Float = 100f,
    pointMode: PointMode = PointMode.Polygon,
    cap: StrokeCap = StrokeCap.Round,
    lineThickness: Float = 1f,
    pixelStep: Float = 1f,
    relativeOffsetX: Float = 0f,
    relativeOffsetY: Float = 0f
) {
    val points = mutableListOf<Offset>()
    var isPositive = true
    var lastY = 0f
    fun Float.fixNaN(): Float {
        return this
    }
    for (x in 0f..size.width step pixelStep) {
        val eqX = (x - relativeOffsetX) / oneUnitToPixelsOfX
        val eqY = -equation.f(eqX).fixNaN()
        val y = eqY * oneUnitToPixelsOfY + relativeOffsetY
        points.add(Offset(x, y))
    }
    drawPoints(
        points = points,
        pointMode = pointMode,
        color = lineColor,
        strokeWidth = lineThickness,
        cap = cap
    )
}

fun interface Equation {
    fun f(x: Float): Float
}


