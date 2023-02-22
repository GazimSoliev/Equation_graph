@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
                val pixelStep = 1f
                EquationGraph(
                    modifier = Modifier.fillMaxSize(),
                    equations = listOf(
                        EquationGraphLine(equation = { it }, lineColor = Color.Blue, pixelStep = pixelStep),
                        EquationGraphLine(equation = { it * it }, lineColor = Color.Red, pixelStep = pixelStep),
                        EquationGraphLine(equation = { sin(it) }, lineColor = Color.Cyan, pixelStep = pixelStep),
                        EquationGraphLine(equation = { cos(it) }, lineColor = Color.Green, pixelStep = pixelStep)
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
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        relativeOffset =
                            Offset(relativeOffset.x + dragAmount.x, relativeOffset.y + dragAmount.y)
                    }
                },
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
    val height = size.height
    val width = size.width
    val centerY = height * ratioY
    val centerX = width * ratioX
    drawEquationGrid(centerX + relativeOffset.x, centerY + relativeOffset.y)
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

fun DrawScope.drawEquationGrid(
    relativeX: Float, relativeY: Float
) {
    fun doSome(relative: Float, length: Float, markupStep: Float): MutableList<Float> {
        val markups = mutableListOf<Float>()
        val relativeGrid = relative % markupStep
        var currentMarkupX = relativeGrid
        while (currentMarkupX > 0f) {
            markups.add(currentMarkupX)
            currentMarkupX -= markupStep
        }
        currentMarkupX = relativeGrid
        while (currentMarkupX < length) {
            markups.add(currentMarkupX)
            currentMarkupX += markupStep
        }
        markups.remove(relative)
        println(markups)
        return markups
    }

    val smallMarkupsX = doSome(relativeX, size.width, 25f)
    val smallMarkupsY = doSome(relativeY, size.height, 25f)
    val markupsX = doSome(relativeX, size.width, 100f)
    val markupsY = doSome(relativeY, size.height, 100f)
    smallMarkupsX.forEach {
        drawLine(
            start = Offset(it, 0f),
            end = Offset(it, size.height),
            color = Color.Black,
            strokeWidth = 1f,
            alpha = 0.05f
        )
    }
    smallMarkupsY.forEach {
        drawLine(
            start = Offset(0f, it),
            end = Offset(size.width, it),
            color = Color.Black,
            strokeWidth = 1f,
            alpha = 0.05f
        )
    }
    markupsX.forEach {
        drawLine(
            start = Offset(it, 0f),
            end = Offset(it, size.height),
            color = Color.Black,
            strokeWidth = 1f,
            alpha = 0.1f
        )
    }
    markupsY.forEach {
        drawLine(
            start = Offset(0f, it),
            end = Offset(size.width, it),
            color = Color.Black,
            strokeWidth = 1f,
            alpha = 0.1f
        )
    }
    if (relativeX in 0f..size.width)
        drawLine(
            start = Offset(relativeX, 0f),
            end = Offset(relativeX, size.height),
            color = Color.Black,
            strokeWidth = 1f,
            alpha = 1f
        )
    if (relativeY in 0f..size.height)
        drawLine(
            start = Offset(0f, relativeY),
            end = Offset(size.width, relativeY),
            color = Color.Black,
            strokeWidth = 1f,
            alpha = 1f
        )
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


