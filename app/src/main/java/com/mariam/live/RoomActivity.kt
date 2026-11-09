
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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

private data class ChatMessage(
    val name: String,
    val message: String,
    val time: String
)

private data class JoinRequest(
    val name: String,
    val seat: Int
)

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
    var speakerOn by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    // للعرض التجريبي: مالك الغرفة
    // لاحقاً نربط الصلاحية بحساب المستخدم والسيرفر
    val isRoomOwner = true

    var pendingRequest by remember {
        mutableStateOf<JoinRequest?>(null)
    }

    var occupiedSeats by remember {
        mutableStateOf(mapOf<Int, String>())
    }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    "Mariam",
                    "أهلاً وسهلاً بالجميع ❤️",
                    "10:34"
                ),
                ChatMessage(
                    "أمير",
                    "مساء الخير للجميع 👋",
                    "10:35"
                ),
                ChatMessage(
                    "نور",
                    "غرفة رائعة جداً 💜",
                    "10:36"
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        // الشريط العلوي
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.ArrowBack,
                    null,
                    tint = White
                )
            }

            Text(
                "MARIAM LIVE",
                color = White,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.MoreVert,
                    null,
                    tint = White
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // بطاقة عنوان الغرفة
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF54208F),
                            Color(0xFF29143F)
                        )
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Text(
                "الغرفة الصوتية الرئيسية",
                color = White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "المضيف: Mariam",
                    color = Muted,
                    fontSize = 15.sp
                )

                Text(
                    "🔥 32.6K     💎 09:28",
                    color = Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // المقاعد
        Text(
            "مقاعد المتحدثين",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (column in 0..3) {
                    val number = row * 4 + column + 1
                    val locked = number == 4 || number == 6 || number == 8
                    val occupant = occupiedSeats[number]

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(116.dp)
                            .background(
                                if (locked) Color(0xFF191023) else Panel,
                                RoundedCornerShape(19.dp)
                            )
                            .clickable {
                                if (!locked && occupant == null &&
                                    pendingRequest == null
                                ) {
                                    pendingRequest =
                                        JoinRequest("ضيف", number)
                                }
                            }
                            .padding(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (locked) {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                tint = Gold,
                                modifier = Modifier.size(27.dp)
                            )
                        } else if (occupant != null) {
                            Icon(
                                Icons.Default.AccountCircle,
                                null,
                                tint = Purple,
                                modifier = Modifier.size(35.dp)
                            )

                            Text(
                                occupant,
                                color = White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { muted = !muted },
                                    modifier = Modifier.size(27.dp)
                                ) {
                                    Icon(
                                        if (muted) Icons.Default.MicOff
                                        else Icons.Default.Mic,
                                        null,
                                        tint = if (muted) Pink else Gold,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        speakerOn = !speakerOn
                                    },
                                    modifier = Modifier.size(27.dp)
                                ) {
                                    Icon(
                                        if (speakerOn)
                                            Icons.Default.VolumeUp
                                        else Icons.Default.VolumeOff,
                                        null,
                                        tint = Purple,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.Add,
                                null,
                                tint = White,
                                modifier = Modifier.size(31.dp)
                            )
                        }

                        Spacer(Modifier.height(5.dp))

                        Text(
                            "مقعد $number",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))

        // إعلان الغرفة
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF482071),
                            Color(0xFF321747)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Text(
                "إعلان الغرفة 📢",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "أهلاً وسهلاً بكم في Mariam Live 💜",
                color = White,
                fontSize = 17.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        // المحادثة
        Text(
            "المحادثة",
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 420.dp)
                .background(
                    Panel,
                    RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            messages.takeLast(30).forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(
                            Color(0xFF302044),
                            RoundedCornerShape(17.dp)
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 13.dp
                        )
                ) {
                    Text(
                        item.name,
                        color = Color(0xFFC18AFF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        item.message,
                        color = White,
                        fontSize = 18.sp,
                        lineHeight = 29.sp
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        item.time,
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // كتابة الرسالة
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Panel,
                        RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 17.dp, vertical = 17.dp)
            ) {
                if (message.isEmpty()) {
                    Text(
                        "اكتب رسالة...",
                        color = Muted,
                        fontSize = 17.sp
                    )
                }

                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = White,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        messages = messages + ChatMessage(
                            "أنت",
                            message,
                            "الآن"
                        )
                        message = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(15.dp)
            ) {
                Icon(Icons.Default.Send, null, tint = White)
            }
        }

        Spacer(Modifier.height(20.dp))

        // أدوات التحكم
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Panel,
                    RoundedCornerShape(22.dp)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                Icons.Default.MicOff,
                if (muted) "المايك مكتوم" else "المايك",
                if (muted) Pink else Purple
            ) {
                muted = !muted
            }

            ControlButton(
                if (speakerOn) Icons.Default.VolumeUp
                else Icons.Default.VolumeOff,
                "السماعة",
                Purple
            ) {
                speakerOn = !speakerOn
            }

            ControlButton(
                Icons.Default.CardGiftcard,
                "هدية",
                Pink
            ) {}

            ControlButton(
                Icons.Default.MoreHoriz,
                "المزيد",
                Purple
            ) {}
        }

        Spacer(Modifier.height(18.dp))

        // طلب الانضمام: يظهر لمالك الغرفة فقط
        if (isRoomOwner && pendingRequest != null) {
            val request = pendingRequest!!

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF302044),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${request.name} يطلب الانضمام",
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "إلى المقعد ${request.seat}",
                        color = Muted,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        occupiedSeats =
                            occupiedSeats + (request.seat to request.name)
                        pendingRequest = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A765)
                    )
                ) {
                    Text("قبول")
                }

                Spacer(Modifier.width(6.dp))

                Button(
                    onClick = {
                        pendingRequest = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD52B50)
                    )
                ) {
                    Text("رفض")
                }
            }
        }

        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            label,
            color = White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
