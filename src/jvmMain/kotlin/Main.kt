import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.math.*

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface {
            Column {
//                Text(
//                    "Hello World!",
//                    modifier = Modifier.fillMaxWidth().border(border = BorderStroke(5.dp, Color.Green))
//                )
//                val lineColor = MaterialTheme.colorScheme.primary
                EquationGraph(
                    modifier = Modifier.fillMaxSize(),
                    equations = listOf(
                        EquationGraphLine(equation = { cos(it) }, lineColor = Color.Red),
                        EquationGraphLine(equation = { it * it }, lineColor = Color.Blue),
                        EquationGraphLine(equation = { sin(it) }, lineColor = Color.Green),
                        EquationGraphLine(equation = { log10(it) }, lineColor = Color.Yellow),
                        EquationGraphLine(equation = { sqrt(3f.pow(2) - it.pow(2)) }, lineColor = Color.Cyan)
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
    modifier: Modifier = Modifier, equations: List<EquationGraphLine>,
    initUnitToPixelsOfX: Float = 100f,
    initUnitToPixelsOfY: Float = 100f,
) = Box(modifier = modifier) {
    val regex = Regex("""[\d.]*""")
    var unitToPixelsOfX by remember { mutableStateOf(initUnitToPixelsOfX) }
    var unitToPixelsOfY by remember { mutableStateOf(initUnitToPixelsOfY) }
    val unitToPixelsOfXY = { (unitToPixelsOfX + unitToPixelsOfY) / 2 }
    var textInTextField by remember { mutableStateOf(unitToPixelsOfXY().toString()) }
    Column {
        EquationGraphCanvas(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            equations.forEach {
                drawEquationLine(
                    equation = it.equation,
                    lineColor = it.lineColor,
                    pointMode = it.pointMode,
                    cap = it.cap,
                    lineThickness = it.lineThickness,
                    pixelStep = it.pixelStep,
                    oneUnitToPixelsOfX = unitToPixelsOfX,
                    oneUnitToPixelsOfY = unitToPixelsOfY
                )
            }
        }
        Row(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp)) {
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    textInTextField,
                    maxLines = 1,
                    modifier = Modifier.width(128.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    onValueChange = { newValue ->
                        if (regex.matches(newValue)) textInTextField = newValue
                        textInTextField.toFloatOrNull()?.takeUnless { it == 0f }?.let {
                            unitToPixelsOfX = it
                            unitToPixelsOfY = it
                        }
                    })
                IconButton(onClick = {
                    unitToPixelsOfX /= 2
                    unitToPixelsOfY /= 2
                    textInTextField = unitToPixelsOfXY().toString()
                }) {
                    Icon(imageVector = Icons.Filled.ZoomOut, "Zoom Out")
                }
                IconButton(onClick = {
                    unitToPixelsOfX *= 2f
                    unitToPixelsOfY *= 2f
                    textInTextField = unitToPixelsOfXY().toString()
                }) {
                    Icon(imageVector = Icons.Filled.ZoomIn, "Zoom In")
                }
            }
        }
    }

}

@Composable
fun EquationGraphCanvas(modifier: Modifier = Modifier, onDraw: DrawScope.() -> Unit) =
    Canvas(
        modifier = modifier.clipToBounds(),
        onDraw = onDraw
    )


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
    relativeOffsetX: Float = 0.5f,
    relativeOffsetY: Float = 0.5f
) {
    val mutableList = mutableListOf<Offset>()
    val width = size.width
    val height = size.height
    val centerX = width * relativeOffsetX
    val centerY = height * relativeOffsetY
//    println("Width: ${size.width}, Height: ${size.height}, CenterX: ${center.x}, CenterY: ${center.y}")
    for (x in -centerX..width - centerX step pixelStep) mutableList.add(
        Offset(
            x,
            equation.f(x / oneUnitToPixelsOfX) * oneUnitToPixelsOfY
        )
    )
    val points = mutableList.map {
        it.copy(x = it.x + centerX, y = -it.y + centerY)
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


