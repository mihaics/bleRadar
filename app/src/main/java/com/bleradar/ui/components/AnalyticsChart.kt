package com.bleradar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
fun LineChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    showPoints: Boolean = true,
    showGrid: Boolean = true,
    title: String = ""
) {
    val density = LocalDensity.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val chartWidth = size.width - 80.dp.toPx()
                val chartHeight = size.height - 80.dp.toPx()
                val leftPadding = 40.dp.toPx()
                val topPadding = 20.dp.toPx()
                
                val maxY = data.maxOfOrNull { it.value } ?: 0f
                val minY = data.minOfOrNull { it.value } ?: 0f
                val yRange = maxY - minY
                
                if (yRange == 0f) return@Canvas
                
                // Draw grid lines
                if (showGrid) {
                    val gridColor = Color(0xFF000000).copy(alpha = 0.3f)
                    
                    // Horizontal grid lines
                    for (i in 0..4) {
                        val y = topPadding + (chartHeight / 4) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(leftPadding + chartWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    // Vertical grid lines
                    val verticalLines = min(data.size, 5)
                    for (i in 0 until verticalLines) {
                        val x = leftPadding + (chartWidth / (verticalLines - 1)) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(x, topPadding),
                            end = Offset(x, topPadding + chartHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                
                // Draw line
                val path = Path()
                val points = data.mapIndexed { index, chartData ->
                    val x = leftPadding + (chartWidth / (data.size - 1)) * index
                    val y = topPadding + chartHeight - ((chartData.value - minY) / yRange) * chartHeight
                    Offset(x, y)
                }
                
                if (points.isNotEmpty()) {
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].x, points[i].y)
                    }
                    
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
                
                // Draw points
                if (showPoints) {
                    points.forEach { point ->
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = backgroundColor,
                            radius = 2.dp.toPx(),
                            center = point
                        )
                    }
                }
                
                // Draw Y-axis labels
                val paint = android.graphics.Paint().apply {
                    color = Color.Black.toArgb()
                    textSize = 12.sp.toPx()
                    isAntiAlias = true
                }
                
                for (i in 0..4) {
                    val value = maxY - (yRange / 4) * i
                    val y = topPadding + (chartHeight / 4) * i
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", value),
                        5.dp.toPx(),
                        y + 5.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    title: String = ""
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val chartWidth = size.width - 80.dp.toPx()
                val chartHeight = size.height - 80.dp.toPx()
                val leftPadding = 40.dp.toPx()
                val topPadding = 20.dp.toPx()
                
                val maxY = data.maxOfOrNull { it.value } ?: 0f
                val minY = 0f
                val yRange = maxY - minY
                
                if (yRange == 0f) return@Canvas
                
                val barWidth = chartWidth / data.size * 0.8f
                val barSpacing = chartWidth / data.size * 0.2f
                
                data.forEachIndexed { index, chartData ->
                    val x = leftPadding + (chartWidth / data.size) * index + barSpacing / 2
                    val barHeight = ((chartData.value - minY) / yRange) * chartHeight
                    val y = topPadding + chartHeight - barHeight
                    
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )
                }
                
                // Draw Y-axis labels
                val paint = android.graphics.Paint().apply {
                    color = Color.Black.toArgb()
                    textSize = 12.sp.toPx()
                    isAntiAlias = true
                }
                
                for (i in 0..4) {
                    val value = maxY - (yRange / 4) * i
                    val y = topPadding + (chartHeight / 4) * i
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.0f", value),
                        5.dp.toPx(),
                        y + 5.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    title: String = ""
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Canvas(
                    modifier = Modifier.size(200.dp)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 * 0.8f
                    
                    val total = data.sumOf { it.value.toDouble() }.toFloat()
                    if (total == 0f) return@Canvas
                    
                    var startAngle = -90f
                    
                    data.forEach { pieData ->
                        val sweepAngle = (pieData.value / total) * 360f
                        
                        drawArc(
                            color = pieData.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                        
                        startAngle += sweepAngle
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    data.forEach { pieData ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(color = pieData.color)
                                }
                            }
                            Text(
                                text = "${pieData.label}: ${pieData.value}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ChartData(
    val label: String,
    val value: Float
)

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)