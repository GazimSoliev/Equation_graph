@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.roundToInt
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
                        EquationGraphLine(equation = { cos(it) }, lineColor = Color.Green, pixelStep = pixelStep),
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
    oneUnitToPixels: Float = 100f,
    mainlineStyle: LineStyle = defaultMainLine(),
    mediumLineStyle: LineStyle = defaultMediumLine(),
    smallLineStyle: LineStyle = defaultSmallLine()
) = Box(modifier = modifier) {
    val regex = Regex("""[\d.]*""")
    var unitToPixels by remember { mutableStateOf(oneUnitToPixels) }
    var textInTextField by remember { mutableStateOf(unitToPixels.toString()) }
    var relativeOffset by remember { mutableStateOf(Offset.Zero) }
    Column {
        EquationGraphCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        relativeOffset =
                            Offset(relativeOffset.x + dragAmount.x, relativeOffset.y + dragAmount.y)
                    }
                },
            equations = equations,
            oneUnitToPixels = unitToPixels,
            relativeOffset = relativeOffset,
            mainlineStyle = mainlineStyle,
            mediumLineStyle = mediumLineStyle,
            smallLineStyle = smallLineStyle
        )
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp),
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
    ratioY: Float = 0.5f,
    mainlineStyle: LineStyle = defaultMainLine(),
    mediumLineStyle: LineStyle = defaultMediumLine(),
    smallLineStyle: LineStyle = defaultSmallLine(),
    textMeasurer: TextMeasurer = rememberTextMeasurer(),
    decimalFormat: DecimalFormat = remember { DecimalFormat("#.#####").apply { roundingMode = RoundingMode.HALF_UP } }
) {
    val animate by animateFloatAsState(
        targetValue = oneUnitToPixels, animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        )
    )
    Canvas(
        modifier = modifier
    ) {
        val height = size.height
        val width = size.width
        val centerY = height * ratioY
        val centerX = width * ratioX
        drawEquationGrid(
            relativeX = centerX + relativeOffset.x,
            relativeY = centerY + relativeOffset.y,
            mainlineStyle = mainlineStyle,
            mediumLineStyle = mediumLineStyle,
            smallLineStyle = smallLineStyle,
            textMeasurer = textMeasurer,
            oneUnitToPixels = oneUnitToPixels,
            decimalFormat = decimalFormat
        )
        equations.forEach {
            drawEquationLine(
                equation = it.equation,
                lineColor = it.lineColor,
                oneUnitToPixelsOfX = animate,
                oneUnitToPixelsOfY = animate,
                pointMode = it.pointMode,
                cap = it.cap,
                lineThickness = it.lineThickness,
                pixelStep = it.pixelStep,
                relativeOffsetX = relativeOffset.x + centerX,
                relativeOffsetY = relativeOffset.y + centerY
            )
        }
    }
}

open class LineStyle(
    val color: Color = Color.Gray,
    val strokeWidth: Float = Stroke.HairlineWidth,
    val cap: StrokeCap = Stroke.DefaultCap,
    val pathEffect: PathEffect? = null,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = DefaultBlendMode
)

@Composable
fun defaultMainLine(
    color: Color = MaterialTheme.colorScheme.onSurface,
    strokeWidth: Float = Stroke.HairlineWidth,
    cap: StrokeCap = Stroke.DefaultCap,
    pathEffect: PathEffect? = null,
    alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DefaultBlendMode
) = LineStyle(
    color = color,
    strokeWidth = strokeWidth,
    cap = cap,
    pathEffect = pathEffect,
    alpha = alpha,
    colorFilter = colorFilter,
    blendMode = blendMode
)

@Composable
fun defaultMediumLine(
    color: Color = MaterialTheme.colorScheme.onSurface,
    strokeWidth: Float = Stroke.HairlineWidth,
    cap: StrokeCap = Stroke.DefaultCap,
    pathEffect: PathEffect? = null,
    alpha: Float = 0.25f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DefaultBlendMode
) = LineStyle(
    color = color,
    strokeWidth = strokeWidth,
    cap = cap,
    pathEffect = pathEffect,
    alpha = alpha,
    colorFilter = colorFilter,
    blendMode = blendMode
)

@Composable
fun defaultSmallLine(
    color: Color = MaterialTheme.colorScheme.onSurface,
    strokeWidth: Float = Stroke.HairlineWidth,
    cap: StrokeCap = Stroke.DefaultCap,
    pathEffect: PathEffect? = null,
    alpha: Float = 0.1f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DefaultBlendMode
) = LineStyle(
    color = color,
    strokeWidth = strokeWidth,
    cap = cap,
    pathEffect = pathEffect,
    alpha = alpha,
    colorFilter = colorFilter,
    blendMode = blendMode
)

fun DrawScope.drawEquationGrid(
    relativeX: Float,
    relativeY: Float,
    mainlineStyle: LineStyle = LineStyle(alpha = 1f),
    mediumLineStyle: LineStyle = LineStyle(alpha = 0.25f),
    smallLineStyle: LineStyle = LineStyle(alpha = 0.1f),
    textMeasurer: TextMeasurer,
    oneUnitToPixels: Float,
    decimalFormat: DecimalFormat
) {

    fun Float.roundToStr() =
        if (this < 1000) decimalFormat.format(this)
        else this.roundToInt().toString()

//    fun String.toAnnotatedString() = buildAnnotatedString { append(this) }


    fun doSome(relative: Float, length: Float, markupStep: Int): List<Float> {
        val size = (length / markupStep).roundToInt()
        val relativeGrid = (relative % markupStep).let { if (it < 0) it + markupStep else it }
        val newMarkups = MutableList(size) { it * markupStep + relativeGrid }
        newMarkups.remove(relative)
        return newMarkups
    }

    val smallMarkupsX = doSome(relativeX, size.width, 25)
    val smallMarkupsY = doSome(relativeY, size.height, 25)
    val markupsX = doSome(relativeX, size.width, 100)
    val markupsY = doSome(relativeY, size.height, 100)

    fun drawLine(lineStyle: LineStyle, start: Offset, end: Offset) = with(lineStyle) {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = cap,
            pathEffect = pathEffect,
            alpha = alpha,
            colorFilter = colorFilter,
            blendMode = blendMode
        )
    }

    fun drawLineVertically(lineStyle: LineStyle, horizontal: Float) =
        drawLine(lineStyle, Offset(horizontal, 0f), Offset(horizontal, size.height))

    fun drawLineHorizontally(lineStyle: LineStyle, vertical: Float) =
        drawLine(lineStyle, Offset(0f, vertical), Offset(size.width, vertical))

    fun fillVertically(lineStyle: LineStyle, list: List<Float>) =
        list.forEach { drawLineVertically(lineStyle, it) }

    fun fillHorizontally(lineStyle: LineStyle, list: List<Float>) =
        list.forEach { drawLineHorizontally(lineStyle, it) }

    fillVertically(smallLineStyle, smallMarkupsX)
    fillHorizontally(smallLineStyle, smallMarkupsY)
    fillVertically(mediumLineStyle, markupsX)
    fillHorizontally(mediumLineStyle, markupsY)
    markupsX.forEach {
        val number = (it - relativeX) / oneUnitToPixels
        val text =
            textMeasurer.measure(buildAnnotatedString {
                append(number.roundToStr())
            })
        drawText(text, topLeft = Offset(if (number < 0) it - text.size.width else it, 0f))
    }
    markupsY.forEach {
        val number = (relativeY - it) / oneUnitToPixels
        val text =
            textMeasurer.measure(buildAnnotatedString {
                append(number.roundToStr())
            })
        drawText(text, topLeft = Offset(size.width - text.size.width, if (number > 0) it - text.size.height else it))
    }
    if (relativeX in 0f..size.width)
        drawLineVertically(mainlineStyle, relativeX)
    if (relativeY in 0f..size.height)
        drawLineHorizontally(mainlineStyle, relativeY)
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
//    var isPositive = true
//    var lastY = 0f
//    fun Float.fixNaN(): Float {
//        return this
//    }
    for (x in 0f..size.width step pixelStep) {
        val eqX = (x - relativeOffsetX) / oneUnitToPixelsOfX
        val eqY = -equation.f(eqX)
//            .fixNaN()
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