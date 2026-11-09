
package com.mariam.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF10091B)
private val Panel = Color(0xFF211632)
private val Purple = Color(0xFF873CFF)
private val Pink = Color(0xFFE23C9A)
private val Gold = Color(0xFFFFD76A)
private val White = Color(0xFFF8F5FF)
private val Muted = Color(0xFFB8ADC9)

class RoomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(16, 9, 27)
        window.navigationBarColor = android.graphics.Color.rgb(16, 9, 27)

        setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    RoomScreen(onClose = { finish() })
                }
            }
        }
    }
}

@Composable
private fun RoomScreen(onClose: () -> Unit) {
    var muted by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(listOf("Mariam: أهلاً وسهلاً بالجميع ❤️"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // الشريط العلوي
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(44.dp)
                    .background(Panel, CircleShape)
            ) {
                Icon(Icons.Default.MoreVert, null, tint = White)
            }

            Text(
                "MARIAM LIVE",
                color = White,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Panel, CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = White)
            }
        }

        Spacer(Modifier.height(18.dp))

        // معلومات الغرفة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF54208F), Color(0xFF29143F))
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "الغرفة الصوتية الرئيسية",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(7.dp))
                Text("المضيف: Mariam", color = Muted, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥 32.6K", color = Gold, fontWeight = FontWeight.Bold)
                Text("💎 09:28", color = White, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(22.dp))

        // المتنافسان
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF482071), Color(0xFF251437))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👑", fontSize = 48.sp)
                Text("Mariam", color = White, fontWeight = FontWeight.Bold)
                Text("🎁 3.2M", color = Gold, fontSize = 13.sp)
            }

            Text("VS", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Black)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛡️", fontSize = 48.sp)
                Text("أمير", color = White, fontWeight = FontWeight.Bold)
                Text("🎁 2.1M", color = Gold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "مقاعد المتحدثين",
            color = White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        // ثمانية مقاعد
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                for (column in 0..3) {
                    val number = row * 4 + column + 1
                    val locked = number == 4 || number == 6 || number == 8

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp)
                            .background(
                                if (locked) Color(0xFF191023) else Panel,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                if (!locked) {
                                    messages = messages + "تم طلب الانضمام إلى المقعد $number"
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (locked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(27.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "مقعد $number",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(14.dp))

        // إعلان الغرفة
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF482071), Color(0xFF321747))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Text("📢 إعلان الغرفة", color = Gold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "أهلاً وسهلاً بكم في Mariam Live 💜",
                color = White,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "المحادثة",
            color = White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel, RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            messages.takeLast(8).forEach { item ->
                Text(
                    item,
                    color = White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // كتابة الرسالة
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Panel, RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                if (message.isEmpty()) {
                    Text("اكتب رسالة...", color = Muted)
                }
                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = White,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(9.dp))

            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        messages = messages + "أنت: $message"
                        message = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 19.dp, vertical = 15.dp)
            ) {
                Icon(Icons.Default.Send, null, tint = White)
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "أدوات التحكم",
            color = White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        // أزرار التحكم
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ControlButton(
                modifier = Modifier.weight(1f),
                icon = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (muted) "المايك مكتوم" else "المايك",
                color = Purple
            ) {
                muted = !muted
            }

            ControlButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.VolumeUp,
                label = "السماعة",
                color = Panel
            ) {}
        }

        Spacer(Modifier.height(10.dp))

        ControlButton(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.CardGiftcard,
            label = "إرسال هدية",
            color = Pink
        ) {}

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ControlButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = White)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
