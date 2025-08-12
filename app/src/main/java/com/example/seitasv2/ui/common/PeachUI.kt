package com.example.seitasv2.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.seitasv2.R

/* ---------- Fondo con degradado melocotón (sin “glow” blanco) ---------- */

@Composable
private fun PeachBackground(modifier: Modifier = Modifier) {
    val peachLight = Color(0xFFF1A07A)
    val peach = Color(0xFFE28763)
    val peachDark = Color(0xFFD07052)
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(peachLight, peach, peachDark))
        )
    )
}

/* ---------- Logo mano con “bobbing” sin tarjeta blanca ---------- */

@Composable
fun HandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val infinite = rememberInfiniteTransition(label = "hand")
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val scale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .offset(y = bob.dp)
            .scale(scale)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_hand),
            contentDescription = "hand",
            modifier = Modifier.height(size),
            contentScale = ContentScale.Fit
        )
    }
}

/* ---------- Botón estilizado ---------- */

@Composable
fun PeachButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEFD)),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF3B2B27)
            )
        }
    }
}

/* ---------- Scaffold con AppBar + fondo + anims ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeachScreen(
    withBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    title: String? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = withBack || actions != null || title != null,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200))
            ) {
                CenterAlignedTopAppBar(
                    title = { if (title != null) Text(title) },
                    navigationIcon = {
                        if (withBack && onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back),
                                    contentDescription = "back"
                                )
                            }
                        }
                    },
                    actions = { actions?.invoke() }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            PeachBackground(Modifier.matchParentSize())
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxSize()
            ) {
                HandLogo()
                content()
            }
        }
    }
}
