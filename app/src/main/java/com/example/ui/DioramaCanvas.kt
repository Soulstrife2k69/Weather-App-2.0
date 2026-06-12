package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.LandmarkItem
import com.example.model.WeatherResponse
import kotlin.math.cos
import kotlin.math.sin

// Bounding box representation for interaction
data class DrawnBounds(
    val landmark: LandmarkItem,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val depth: Double
)

@Composable
fun DioramaCanvas(
    weatherData: WeatherResponse,
    selectedLandmark: LandmarkItem?,
    onLandmarkSelected: (LandmarkItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Colors
    val skyColorStart = remember(weatherData.scenery.skyColorStart) {
        parseHexColor(weatherData.scenery.skyColorStart, Color(0xFF4BB4E8))
    }
    val skyColorEnd = remember(weatherData.scenery.skyColorEnd) {
        parseHexColor(weatherData.scenery.skyColorEnd, Color(0xFFBBE6FA))
    }

    // Interactive drawn models
    val drawnBoundsList = remember { mutableStateListOf<DrawnBounds>() }

    // Animations for weather ambience
    val infiniteTransition = rememberInfiniteTransition(label = "weather_effects")

    // Cloud motion offset
    val cloudOffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud_motion"
    )

    // Rain drop vertical animation fraction
    val rainFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_motion"
    )

    // Twinkling stars phase
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    // Interactive tap detection
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(weatherData) {
                detectTapGestures { tapOffset ->
                    // Search in reverse order (foreground first) to handle overlapping elements correctly
                    val tapped = drawnBoundsList
                        .sortedByDescending { it.depth }
                        .firstOrNull { bounds ->
                            tapOffset.x >= bounds.left && tapOffset.x <= bounds.right &&
                                    tapOffset.y >= bounds.top && tapOffset.y <= bounds.bottom
                        }
                    
                    if (tapped != null) {
                        onLandmarkSelected(tapped.landmark)
                    } else {
                        onLandmarkSelected(null) // Tap on sky/ground clears selection
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Draw Sky Background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyColorStart, skyColorEnd),
                startY = 0f,
                endY = height * 0.70f
            ),
            size = Size(width, height)
        )

        // Determine if it is Night / Dark Evening to draw stars
        val isDarkSky = isSkyColorDark(skyColorStart)
        if (isDarkSky) {
            drawStarField(width, height * 0.70f, starTwinkle)
            drawCrescentMoon(width, height * 0.70f)
        }

        // Draw Sky Weather Animations
        val condition = weatherData.weather.condition.lowercase()
        if (condition.contains("rain") || condition.contains("drizzle") || condition.contains("storm")) {
            drawRainDrops(width, height * 0.70f, rainFraction)
        } else if (condition.contains("snow") || condition.contains("ice") || condition.contains("freeze")) {
            drawSnowFlakes(width, height * 0.70f, rainFraction)
        } else {
            // Draw floating beautiful clouds for sunny/cloudy conditions
            drawFloatingClouds(width, height * 0.70f, cloudOffsetFraction, condition.contains("cloud"))
        }

        // 2. Draw 3D Ground Horizon Plane
        val horizonY = height * 0.70f
        val groundHeight = height - horizonY

        // Radial or vertical gradient representing the ground depth
        val groundColorStart = Color(0xFF1E293B)  // Slate-800
        val groundColorEnd = Color(0xFF0F172A)    // Slate-900

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(groundColorStart, groundColorEnd),
                startY = horizonY,
                endY = height
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(width, groundHeight)
        )

        // Draw subtle perspective lines on ground
        drawGroundGrid(width, height, horizonY)

        // 3. Draw Landmark Diorama Cluster
        val landmarks = weatherData.scenery.landmarks
        // Sort landmarks by depth level (background 1.0 to foreground 5.0)
        val sortedLandmarks = landmarks.sortedBy { it.depth }

        // Clear previous bounding box list
        drawnBoundsList.clear()

        sortedLandmarks.forEach { item ->
            // Base math matching description
            val depthFactor = item.depth.toFloat()
            val depthSpacing = 8.dp.toPx() // Vertical descent for foreground perspective layering
            
            val baseY = horizonY + (depthFactor - 1f) * depthSpacing
            val centerX = width * (item.x.toFloat() / 100f)
            val elemWidth = width * (item.width.toFloat() / 100f)
            val elemHeight = (height * 0.55f) * (item.height.toFloat() / 100f)

            // Extrusion offsets for 3D axonometric block effect
            // Far depth (1.0) has small offset, foreground (5.0) has larger, more pronounced offset!
            val extX = (depthFactor * 4.dp.toPx())
            val extY = -(depthFactor * 3.dp.toPx())

            val leftX = centerX - elemWidth / 2f
            val rightX = centerX + elemWidth / 2f
            val topY = baseY - elemHeight

            // Store bounding box for click interaction (including the extrusion bounds)
            val actualLeft = minOf(leftX, leftX + extX)
            val actualRight = maxOf(rightX, rightX + extX)
            val actualTop = minOf(topY, topY + extY)
            val actualBottom = baseY

            drawnBoundsList.add(
                DrawnBounds(
                    landmark = item,
                    left = actualLeft,
                    right = actualRight,
                    top = actualTop,
                    bottom = actualBottom,
                    depth = item.depth
                )
            )

            // Setup colors
            val baseColor = parseHexColor(item.colorHex, Color(0xFF70624E))
            val isSelected = selectedLandmark?.name == item.name

            // Glow/outline color if selected
            val finalBaseColor = if (isSelected) {
                getHighlightedColor(baseColor, 0.25f)
            } else {
                baseColor
            }

            // Shade variants to represent standard lighting model (light source from top-left direction)
            val frontColor = finalBaseColor
            val sideColor = getShadedColor(finalBaseColor, 0.72f) // 28% darker side face
            val topColor = getHighlightedColor(finalBaseColor, 0.20f) // 20% brighter top face

            // Draw specific shape models
            when (item.shapeType.lowercase()) {
                "pyramid" -> {
                    draw3DPyramid(leftX, rightX, centerX, topY, baseY, extX, extY, frontColor, sideColor)
                }
                "spire" -> {
                    draw3DSpire(leftX, rightX, centerX, topY, baseY, extX, extY, frontColor, sideColor, topColor)
                }
                "tower" -> {
                    draw3DTaperedTower(leftX, rightX, centerX, topY, baseY, extX, extY, frontColor, sideColor, topColor, elemWidth)
                }
                "dome" -> {
                    draw3DDome(leftX, rightX, centerX, topY, baseY, extX, extY, frontColor, sideColor, topColor, elemWidth, elemHeight)
                }
                "arch" -> {
                    draw3DArch(leftX, rightX, centerX, topY, baseY, extX, extY, frontColor, sideColor, topColor, elemWidth, elemHeight)
                }
                else -> { // "box" or default
                    draw3DBox(leftX, rightX, topY, baseY, extX, extY, frontColor, sideColor, topColor)
                }
            }

            // Draw selection halo/accent if clicked
            if (isSelected) {
                drawSelectionHighlight(leftX, rightX, topY, baseY, extX, extY)
            }
        }
    }
}

// --- Shader and Shading Helpers ---

fun getShadedColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

fun getHighlightedColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

fun parseHexColor(hexString: String, defaultColor: Color): Color {
    return try {
        val cleanHex = hexString.trim().replace("#", "")
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

// Determines if sky is dark to draw cozy stars & moon
fun isSkyColorDark(skyColor: Color): Boolean {
    // Standard luminance formula
    val luminance = 0.299f * skyColor.red + 0.587f * skyColor.green + 0.114f * skyColor.blue
    return luminance < 0.45f
}

// --- Core 2D/3D Mesh Drawing Functions ---

private fun DrawScope.drawSelectionHighlight(
    left: Float, right: Float, top: Float, bottom: Float,
    extX: Float, extY: Float
) {
    val highlightColor = Color(0xFF38BDF8) // Light blue glow
    val strokeWidth = 3.dp.toPx()

    // Outlining the full 3D silhouette path
    val outlinePath = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top)
        lineTo(left + extX, top + extY)
        lineTo(right + extX, top + extY)
        lineTo(right + extX, bottom + extY)
        lineTo(right, bottom)
        close()
    }

    drawPath(
        path = outlinePath,
        color = highlightColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.draw3DBox(
    leftX: Float, rightX: Float, topY: Float, baseY: Float,
    extX: Float, extY: Float,
    frontColor: Color, sideColor: Color, topColor: Color
) {
    // 1. Draw Side Face (Right)
    val sidePath = Path().apply {
        moveTo(rightX, baseY)
        lineTo(rightX + extX, baseY + extY)
        lineTo(rightX + extX, topY + extY)
        lineTo(rightX, topY)
        close()
    }
    drawPath(path = sidePath, color = sideColor)

    // 2. Draw Top Face
    val topPath = Path().apply {
        moveTo(leftX, topY)
        lineTo(leftX + extX, topY + extY)
        lineTo(rightX + extX, topY + extY)
        lineTo(rightX, topY)
        close()
    }
    drawPath(path = topPath, color = topColor)

    // 3. Draw Front Face
    drawRect(
        color = frontColor,
        topLeft = Offset(leftX, topY),
        size = Size(rightX - leftX, baseY - topY)
    )
}

private fun DrawScope.draw3DPyramid(
    leftX: Float, rightX: Float, centerX: Float, topY: Float, baseY: Float,
    extX: Float, extY: Float,
    frontColor: Color, sideColor: Color
) {
    // Front Left Face
    val frontLeftPath = Path().apply {
        moveTo(leftX, baseY)
        lineTo(centerX, topY)
        lineTo(centerX, baseY)
        close()
    }
    drawPath(path = frontLeftPath, color = frontColor)

    // Front Right Face
    val frontRightPath = Path().apply {
        moveTo(centerX, baseY)
        lineTo(centerX, topY)
        lineTo(rightX, baseY)
        close()
    }
    drawPath(path = frontRightPath, color = getShadedColor(frontColor, 0.88f))

    // Shaded 3D Extrusion Side Face
    val sidePath = Path().apply {
        moveTo(rightX, baseY)
        lineTo(rightX + extX, baseY + extY)
        lineTo(centerX + extX, topY + extY)
        lineTo(centerX, topY)
        close()
    }
    drawPath(path = sidePath, color = sideColor)
}

private fun DrawScope.draw3DSpire(
    leftX: Float, rightX: Float, centerX: Float, topY: Float, baseY: Float,
    extX: Float, extY: Float,
    frontColor: Color, sideColor: Color, topColor: Color
) {
    // Spires have tall steep faces with sharp highlight
    val leftPath = Path().apply {
        moveTo(leftX, baseY)
        lineTo(centerX, topY)
        lineTo(centerX, baseY)
        close()
    }
    drawPath(path = leftPath, color = topColor)

    val rightPath = Path().apply {
        moveTo(centerX, baseY)
        lineTo(centerX, topY)
        lineTo(rightX, baseY)
        close()
    }
    drawPath(path = rightPath, color = frontColor)

    val sidePath = Path().apply {
        moveTo(rightX, baseY)
        lineTo(rightX + extX, baseY + extY)
        lineTo(centerX + extX, topY + extY)
        lineTo(centerX, topY)
        close()
    }
    drawPath(path = sidePath, color = sideColor)
}

private fun DrawScope.draw3DTaperedTower(
    leftX: Float, rightX: Float, centerX: Float, topY: Float, baseY: Float,
    extX: Float, extY: Float,
    frontColor: Color, sideColor: Color, topColor: Color,
    widthPx: Float
) {
    // Tapered towers have narrower top (like Eiffel or pagoda base structures)
    val topW = widthPx * 0.35f
    val topLeftX = centerX - topW / 2
    val topRightX = centerX + topW / 2

    // 1. Draw Side Face (Right)
    val sidePath = Path().apply {
        moveTo(rightX, baseY)
        lineTo(rightX + extX, baseY + extY)
        lineTo(topRightX + extX, topY + extY)
        lineTo(topRightX, topY)
        close()
    }
    drawPath(path = sidePath, color = sideColor)

    // 2. Draw Top Face
    val topPath = Path().apply {
        moveTo(topLeftX, topY)
        lineTo(topLeftX + extX, topY + extY)
        lineTo(topRightX + extX, topY + extY)
        lineTo(topRightX, topY)
        close()
    }
    drawPath(path = topPath, color = topColor)

    // 3. Draw Front Face
    val frontPath = Path().apply {
        moveTo(leftX, baseY)
        lineTo(topLeftX, topY)
        lineTo(topRightX, topY)
        lineTo(rightX, baseY)
        close()
    }
    drawPath(path = frontPath, color = frontColor)

    // Add a structural spire on top of the tower automatically
    val beaconTopY = topY - (baseY - topY) * 0.25f
    val beaconLeft = centerX - topW * 0.15f
    val beaconRight = centerX + topW * 0.15f
    draw3DSpire(beaconLeft, beaconRight, centerX, beaconTopY, topY, extX * 0.4f, extY * 0.4f, topColor, sideColor, topColor)
}

private fun DrawScope.draw3DDome(
    leftX: Float, rightX: Float, centerX: Float, topY: Float, baseY: Float,
    extX: Float, extY: Float,
    frontColor: Color, sideColor: Color, topColor: Color,
    widthPx: Float, heightPx: Float
) {
    // Domes are rendered as rounded shapes. To render 3D dome beautifully,
    // we combine a square base with a curved dome top brushed with radial light.
    val baseHeight = heightPx * 0.3f
    val transitionY = baseY - baseHeight

    // Draw cylindrical base
    draw3DBox(leftX, rightX, transitionY, baseY, extX, extY, frontColor, sideColor, topColor)

    // Draw dome top cap
    val domePath = Path().apply {
        moveTo(leftX, transitionY)
        cubicTo(
            leftX, transitionY - (heightPx * 0.85f),
            rightX, transitionY - (heightPx * 0.85f),
            rightX, transitionY
        )
        close()
    }

    // Radial gradient centered upper-left to mimic a polished light sphere
    val sphereBrush = Brush.radialGradient(
        colors = listOf(topColor, frontColor, sideColor),
        center = Offset(centerX - widthPx * 0.15f, transitionY - heightPx * 0.35f),
        radius = widthPx * 0.6f
    )

    // Clip and draw dome
    drawPath(path = domePath, brush = sphereBrush)

    // Draw dome 3D extrusion edge back curve
    val domeBackPath = Path().apply {
        moveTo(rightX, transitionY)
        cubicTo(
            rightX, transitionY - (heightPx * 0.85f),
            rightX, transitionY, // fallback anchor
            rightX, transitionY
        )
        lineTo(rightX + extX, transitionY + extY)
        close()
    }
    drawPath(path = domeBackPath, color = sideColor)

    // Add a tiny pinnacle spire on top of dome
    val pinnacleTopY = transitionY - heightPx * 0.7f
    val pinnacleLeft = centerX - 3.dp.toPx()
    val pinnacleRight = centerX + 3.dp.toPx()
    draw3DSpire(pinnacleLeft, pinnacleRight, centerX, pinnacleTopY, transitionY - heightPx * 0.65f, extX * 0.2f, extY * 0.2f, topColor, sideColor, topColor)
}

private fun DrawScope.draw3DArch(
    leftX: Float, rightX: Float, centerX: Float, topY: Float, baseY: Float,
    extX: Float, extY: Float,
    frontColor: Color, sideColor: Color, topColor: Color,
    widthPx: Float, heightPx: Float
) {
    // Underpinning Arch (e.g. Arc de Triomphe, Roman Arches)
    val archH = heightPx * 0.5f
    val archW = widthPx * 0.45f
    val archLeft = centerX - archW / 2
    val archRight = centerX + archW / 2

    // 1. Draw Side Face
    val sidePath = Path().apply {
        moveTo(rightX, baseY)
        lineTo(rightX + extX, baseY + extY)
        lineTo(rightX + extX, topY + extY)
        lineTo(rightX, topY)
        close()
    }
    drawPath(path = sidePath, color = sideColor)

    // 2. Draw Top Face
    val topPath = Path().apply {
        moveTo(leftX, topY)
        lineTo(leftX + extX, topY + extY)
        lineTo(rightX + extX, topY + extY)
        lineTo(rightX, topY)
        close()
    }
    drawPath(path = topPath, color = topColor)

    // 3. Draw Front Face with Arch Cutout
    val frontArchPath = Path().apply {
        moveTo(leftX, baseY)
        lineTo(leftX, topY)
        lineTo(rightX, topY)
        lineTo(rightX, baseY)
        lineTo(archRight, baseY)
        // Arch Roof curve
        cubicTo(
            archRight, baseY - archH * 1.3f,
            archLeft, baseY - archH * 1.3f,
            archLeft, baseY
        )
        lineTo(leftX, baseY)
        close()
    }
    drawPath(path = frontArchPath, color = frontColor)

    // 4. Draw Inside Arch Extrusion (adds extreme depth and realism!)
    val insideArchShade = Path().apply {
        moveTo(archLeft, baseY)
        cubicTo(
            archLeft, baseY - archH * 1.3f,
            archRight, baseY - archH * 1.3f,
            archRight, baseY
        )
        lineTo(archRight + extX, baseY + extY)
        cubicTo(
            archRight + extX, baseY + extY - archH * 1.3f,
            archLeft + extX, baseY + extY - archH * 1.3f,
            archLeft + extX, baseY + extY
        )
        close()
    }
    drawPath(path = insideArchShade, color = getShadedColor(sideColor, 0.7f))
}

// --- Environment effects & Animations ---

private fun DrawScope.drawStarField(width: Float, skyHeight: Float, starTwinkle: Float) {
    // Fixed coordinates for 40 distant cute stars
    val stars = listOf(
        Offset(0.12f, 0.15f), Offset(0.28f, 0.08f), Offset(0.42f, 0.22f), Offset(0.55f, 0.11f),
        Offset(0.73f, 0.05f), Offset(0.85f, 0.26f), Offset(0.92f, 0.14f), Offset(0.04f, 0.35f),
        Offset(0.22f, 0.40f), Offset(0.64f, 0.32f), Offset(0.79f, 0.44f), Offset(0.35f, 0.48f),
        Offset(0.48f, 0.55f), Offset(0.95f, 0.48f), Offset(0.15f, 0.58f), Offset(0.58f, 0.62f),
        Offset(0.88f, 0.60f), Offset(0.02f, 0.10f), Offset(0.38f, 0.04f), Offset(0.68f, 0.20f)
    )

    stars.forEachIndexed { index, starPoint ->
        val x = starPoint.x * width
        val y = starPoint.y * skyHeight
        
        // Twinkling animation intensity
        val intensity = if (index % 2 == 0) starTwinkle else (1.3f - starTwinkle)
        val starRadius = (1.5.dp + (index % 3).dp).toPx() * intensity

        drawCircle(
            color = Color.White.copy(alpha = 0.82f * intensity),
            radius = starRadius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawCrescentMoon(width: Float, skyHeight: Float) {
    val centerX = width * 0.85f
    val centerY = skyHeight * 0.22f
    val radius = 24.dp.toPx()

    // Draw outer bright yellow moon halo
    drawCircle(
        color = Color(0xFFFEF08A).copy(alpha = 0.25f),
        radius = radius + 6.dp.toPx(),
        center = Offset(centerX, centerY)
    )

    // Draw primary crescent using path subtraction/clip
    val outerPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius))
    }
    // Shorter oval offset to cut into the circle
    val innerPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(centerX - radius + 12.dp.toPx(), centerY - radius - 4.dp.toPx(), centerX + radius + 16.dp.toPx(), centerY + radius + 4.dp.toPx()))
    }
    val crescentPath = Path().apply {
        op(outerPath, innerPath, PathOperation.Difference)
    }

    drawPath(path = crescentPath, color = Color(0xFFFEF08A)) // Cozy yellow
}

private fun DrawScope.drawFloatingClouds(
    width: Float, skyHeight: Float,
    offsetFraction: Float, isCloudy: Boolean
) {
    // Generate horizontal positions based on animation offset
    val cloudsCount = if (isCloudy) 6 else 3
    val cloudColor = Color.White.copy(alpha = if (isCloudy) 0.5f else 0.32f)

    for (i in 0 until cloudsCount) {
        val baseSpeedMultiplier = 1f + (i * 0.2f)
        val startX = (-120.dp.toPx()) + ((width + 240.dp.toPx()) * (offsetFraction * baseSpeedMultiplier)).rem(width + 240.dp.toPx())
        val centerY = skyHeight * (0.15f + (i * 0.12f))
        val scaleW = 50.dp.toPx() + (i * 12.dp.toPx())
        val scaleH = 20.dp.toPx() + (i * 4.dp.toPx())

        // Draw overlapping fluffy ovals representing a high density cloud block
        drawCircle(
            color = cloudColor,
            radius = scaleH * 1.3f,
            center = Offset(startX, centerY)
        )
        drawCircle(
            color = cloudColor,
            radius = scaleH * 0.9f,
            center = Offset(startX - scaleW * 0.4f, centerY + scaleH * 0.2f)
        )
        drawCircle(
            color = cloudColor,
            radius = scaleH * 1.0f,
            center = Offset(startX + scaleW * 0.4f, centerY + scaleH * 0.1f)
        )
    }
}

private fun DrawScope.drawRainDrops(width: Float, skyHeight: Float, rainFraction: Float) {
    val dropColor = Color(0x99A5F3FC) // Cyan translucent rain
    val linesCount = 45

    for (i in 0 until linesCount) {
        val xSeed = (i * 19) % 100
        val ySeed = (i * 31) % 100
        val startX = width * (xSeed / 100f) + (rainFraction * 15f)
        val startY = skyHeight * (ySeed / 100f) + (rainFraction * skyHeight)
        
        val actualY = startY.rem(skyHeight)
        val actualX = startX.rem(width)

        drawLine(
            color = dropColor,
            start = Offset(actualX, actualY),
            end = Offset(actualX - 4.dp.toPx(), actualY + 12.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

private fun DrawScope.drawSnowFlakes(width: Float, skyHeight: Float, fraction: Float) {
    val flakeColor = Color.White.copy(alpha = 0.85f)
    val flakesCount = 40

    for (i in 0 until flakesCount) {
        val xSeed = (i * 23) % 100
        val ySeed = (i * 17) % 100
        // Drift sideways like a real sinusoidal snow flow
        val waveOffset = sin(fraction * 2 * Math.PI.toFloat() + i) * 10.dp.toPx()
        val startX = width * (xSeed / 100f) + waveOffset
        val startY = skyHeight * (ySeed / 100f) + (fraction * skyHeight)

        val actualY = startY.rem(skyHeight)
        val actualX = (startX + width).rem(width) // wrap safe
        val radius = (1.5.dp + (i % 3).dp).toPx()

        drawCircle(
            color = flakeColor,
            radius = radius,
            center = Offset(actualX, actualY)
        )
    }
}

private fun DrawScope.drawGroundGrid(width: Float, height: Float, horizonY: Float) {
    val gridColor = Color(0xFF334155).copy(alpha = 0.35f) // Slate-700
    val count = 12

    // 1. Draw horizontal depth convergence lines (perspective spacing!)
    // Lines scale logarithmic / quadratic so they match perspective geometry!
    for (i in 1..8) {
        val ratio = i / 8f
        val calculatedY = horizonY + (groundGridPerspectiveCurve(ratio) * (height - horizonY))
        drawLine(
            color = gridColor,
            start = Offset(0f, calculatedY),
            end = Offset(width, calculatedY),
            strokeWidth = (0.75f + ratio * 1.5f).dp.toPx()
        )
    }

    // 2. Draw radiating architectural lines converging at the horizon center
    val vanishingPointX = width / 2f
    for (i in -count..count) {
        val fraction = i / count.toFloat()
        val groundEndX = vanishingPointX + fraction * width * 1.5f
        drawLine(
            color = gridColor,
            start = Offset(vanishingPointX + fraction * 30.dp.toPx(), horizonY),
            end = Offset(groundEndX, height),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// Gives nice depth scaling: lines are very dense near horizon, spaced out near front!
fun groundGridPerspectiveCurve(input: Float): Float {
    return input * input // quadratic progression
}
