package com.shieldguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldguard.app.ui.theme.*

/**
 * Premium Glassy UI Switch / Toggle with physical spring dynamics and specular rim lighting.
 */
@Composable
fun GlassySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth physical spring for thumb translation
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbOffset"
    )

    // Emil Kowalski principle: buttons stretch/respond on press
    val thumbWidth by animateDpAsState(
        targetValue = if (isPressed) 26.dp else 22.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "thumbWidth"
    )

    // Dynamic track background gradient
    val trackStartColor by animateColorAsState(
        targetValue = if (checked) Color(0x6610B981) else Color(0x1AFFFFFF),
        animationSpec = tween(durationMillis = 200),
        label = "trackStart"
    )
    val trackEndColor by animateColorAsState(
        targetValue = if (checked) Color(0x3300E5FF) else Color(0x0AFFFFFF),
        animationSpec = tween(durationMillis = 200),
        label = "trackEnd"
    )

    // Specular border reflection
    val borderStartColor by animateColorAsState(
        targetValue = if (checked) Color(0xAA10B981) else Color(0x35FFFFFF),
        animationSpec = tween(durationMillis = 200),
        label = "borderStart"
    )
    val borderEndColor by animateColorAsState(
        targetValue = if (checked) Color(0x5500E5FF) else Color(0x10FFFFFF),
        animationSpec = tween(durationMillis = 200),
        label = "borderEnd"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(trackStartColor, trackEndColor)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderStartColor, borderEndColor)
                ),
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Inner glow diffusion when checked
        if (checked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x3010B981), Color.Transparent),
                            radius = 60f
                        )
                    )
            )
        }

        // Floating Glass Thumb Orb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(width = thumbWidth, height = 22.dp)
                .shadow(
                    elevation = if (checked) 6.dp else 3.dp,
                    shape = CircleShape,
                    spotColor = if (checked) NeonEmerald else Color.Black
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = if (checked) {
                            listOf(Color(0xFFFFFFFF), Color(0xFFE0F2FE))
                        } else {
                            listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1))
                        }
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = if (checked) Color(0x80FFFFFF) else Color(0x40FFFFFF),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Frosted Glass Card with specular gradient rim reflection and depth.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassFillStandard,
    borderColor: Color = GlassBorderHairline,
    specularHighlight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (specularHighlight) {
        Brush.verticalGradient(
            colors = listOf(
                GlassBorderSpecular,
                GlassBorderHairline,
                Color(0x05FFFFFF)
            )
        )
    } else {
        Brush.linearGradient(listOf(borderColor, borderColor))
    }

    Card(
        modifier = modifier
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

/**
 * Real-time breathing pulse aura for the active shield.
 */
@Composable
fun StatusPulseIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.7f else 0.3f,
        targetValue = if (isActive) 0.1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(NeonEmerald.copy(alpha = pulseAlpha))
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (isActive) NeonEmerald else TextTertiary)
                .border(
                    width = 1.dp,
                    color = if (isActive) Color(0x80FFFFFF) else Color.Transparent,
                    shape = CircleShape
                )
        )
    }
}

/**
 * Apple-style floating glassy segmented control with gliding pill indicator.
 */
@Composable
fun GlassSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(GlassFillStandard)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(GlassBorderSpecular, GlassBorderHairline)
                ),
                shape = RoundedCornerShape(23.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val transitionColor by animateColorAsState(
                targetValue = if (isSelected) GlassFillActive else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) TextPrimary else TextSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "tabText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(19.dp))
                    .background(transitionColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0x60FFFFFF), Color(0x1AFFFFFF))
                                ),
                                shape = RoundedCornerShape(19.dp)
                            )
                        } else Modifier
                    )
                    .clickable { onItemSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Tactile Glass Button with Emil Kowalski press physics.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = CyberCyan,
    contentColor: Color = BackgroundObsidian,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = GlassFillStandard,
            disabledContentColor = TextTertiary
        ),
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        content = content
    )
}
