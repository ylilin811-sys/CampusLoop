package com.example.homework2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Particle(
    val xRatio: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)

@Composable
fun ConfettiCanvas(
    modifier: Modifier = Modifier,
    particleCount: Int = 40
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFF8A3D), Color(0xFFFFD166), Color(0xFF06D6A0),
            Color(0xFF118AB2), Color(0xFFEF476F), Color(0xFF9C27B0)
        )
        List(particleCount) {
            Particle(
                xRatio = Random.nextFloat(),
                speed = Random.nextFloat() * 0.8f + 0.4f,
                size = Random.nextFloat() * 16f + 12f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = Random.nextFloat() * 360f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { p ->
            val currentY = ((progress * p.speed) % 1f) * (canvasHeight + 100f) - 50f
            val currentX = p.xRatio * canvasWidth
            drawRect(
                color = p.color,
                topLeft = Offset(currentX, currentY),
                size = Size(p.size, p.size * 1.5f)
            )
        }
    }
}
