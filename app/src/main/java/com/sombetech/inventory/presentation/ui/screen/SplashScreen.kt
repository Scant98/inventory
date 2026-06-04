package com.sombetech.inventory.presentation.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sombetech.inventory.presentation.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // ── Entrance animations ───────────────────────────────────────────────────
    val cardScale    = remember { Animatable(0.55f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Icon card bounces in
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow,
            ),
        )
        // Text fades in right after
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        )
        delay(1_400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF5A52E8), ClayPrimary, ClayPrimaryEnd))
            ),
    ) {
        // ── Decorative background blobs ───────────────────────────────────────
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = 100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        Box(
            Modifier
                .size(240.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        Box(
            Modifier
                .size(160.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = (-120).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )

        // ── Centre content ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Clay icon card
            Box(
                modifier = Modifier
                    .scale(cardScale.value)
                    .size(128.dp)
                    .clayShadow(
                        shadowColor  = Color.Black,
                        borderRadius = 40.dp,
                        blurRadius   = 40.dp,
                        offsetY      = 16.dp,
                        shadowAlpha  = 0.30f,
                    )
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                // Inner gradient tint behind icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(ClayPurpLight, ClayBlueLight))
                        ),
                )
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = ClayPrimary,
                    modifier = Modifier.size(68.dp),
                )
            }

            Spacer(Modifier.height(36.dp))

            // App name
            Text(
                text = "Inventory Pro",
                color = Color.White.copy(alpha = contentAlpha.value),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Smart stock management",
                color = Color.White.copy(alpha = contentAlpha.value * 0.70f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(48.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.then(
                    if (contentAlpha.value > 0.5f) Modifier else Modifier
                ),
            ) {
                repeat(3) { index ->
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (contentAlpha.value > 0.5f) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis    = index * 120,
                        ),
                        label = "dot$index",
                    )
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = dotAlpha * 0.55f))
                    )
                }
            }
        }

        // ── Version pill at bottom ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    "v 1.0",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
