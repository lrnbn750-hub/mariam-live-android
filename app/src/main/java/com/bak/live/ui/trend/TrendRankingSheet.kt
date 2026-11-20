package com.bak.live.ui.trend // عدّل الحزمة لتطابق مشروعك

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * تصميم فقط (UI only).
 * لا يحسب نقاطاً ولا يتصل بأي API. كل النصوص والأرقام تأتي جاهزة من بيانات التطبيق الحالية.
 */
@Immutable
data class TrendUiUser(
    val id: String,
    val name: String,
    val scoreText: String,   // النص كما يعرضه التطبيق حالياً، مثل 39.3k
    val rankLabel: String,   // مثل 4 أو +999 (من البيانات الفعلية)
)

private val TrendMedalShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height * 0.25f)
    lineTo(size.width * 0.88f, size.height * 0.78f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(size.width * 0.12f, size.height * 0.78f)
    lineTo(0f, size.height * 0.25f)
    close()
}

private object TrendColors {
    val Sheet = Color.White
    val TextMain = Color(0xFF1F1F1F)
    val TextSub = Color(0xFF9A9A9A)
    val Teal = Color(0xFF14C8B4)
    val Fire = Color(0xFFFF7A1A)

    val GoldTop = Color(0xFFFFE98A)
    val GoldBottom = Color(0xFFFFF8DC)
    val GoldRing = Color(0xFFF2B600)

    val SilverTop = Color(0xFFDDE6FF)
    val SilverBottom = Color(0xFFF4F7FF)
    val SilverRing = Color(0xFF9DB4E8)

    val BronzeTop = Color(0xFFFFDDD2)
    val BronzeBottom = Color(0xFFFFF3EE)
    val BronzeRing = Color(0xFFD9915E)
}

/**
 * @param topUsers      أول ثلاثة بترتيبهم (الأول، الثاني، الثالث). يقبل أقل من ثلاثة.
 * @param restUsers     من المركز الرابع فما بعد.
 * @param currentUser   صف المستخدم الحالي المثبّت؛ مرّر null لإخفائه.
 * @param avatar        خانة لعرض الصورة بمكتبة الصور الحالية عندك (Coil / Glide...).
 *
 * مثال الاستخدام داخل شاشة الغرفة:
 * Box(Modifier.fillMaxSize()) {
 *     /* محتوى الغرفة */
 *     TrendRankingSheet(
 *         modifier = Modifier
 *             .align(Alignment.BottomCenter)
 *             .fillMaxHeight(0.68f),
 *         ...
 *     )
 * }
 */
@Composable
fun TrendRankingSheet(
    topUsers: List<TrendUiUser>,
    restUsers: List<TrendUiUser>,
    currentUser: TrendUiUser?,
    avatar: @Composable (user: TrendUiUser, modifier: Modifier) -> Unit,
    onClose: () -> Unit,
    onUserClick: (TrendUiUser) -> Unit,
    modifier: Modifier = Modifier,
    onRulesClick: (() -> Unit)? = null,
    onDailyTasksClick: (() -> Unit)? = null, // مرّره فقط إذا كان التبويب موجوداً في التطبيق
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = TrendColors.Sheet,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                SheetHeader(onClose, onDailyTasksClick)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
                ) {
                    item { RulesLink(onRulesClick) }
                    item { Podium(topUsers, avatar, onUserClick) }
                    item { Spacer(Modifier.height(8.dp)) }
                    items(restUsers, key = { it.id }) { user ->
                        RankRow(user, avatar, onUserClick)
                    }
                }

                if (currentUser != null) {
                    CurrentUserBar(currentUser, avatar, onUserClick)
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(onClose: () -> Unit, onDailyTasksClick: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
        ) {
            // اليمين (بداية RTL): المهام اليومية إن وُجدت
            if (onDailyTasksClick != null) {
                Text(
                    text = "المهام اليومية",
                    color = TrendColors.TextSub,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(onClick = onDailyTasksClick),
                )
            }
            // الوسط: العنوان + أيقونة الأعمدة
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = TrendColors.Teal,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "قائمة المستخدمين المشهورين",
                    color = TrendColors.TextMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // اليسار (نهاية RTL): زر الإغلاق
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "إغلاق",
                tint = TrendColors.TextMain,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(26.dp)
                    .clickable(onClick = onClose),
            )
        }
        // مؤشر السحب الصغير أسفل العنوان
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp)
                .width(14.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(TrendColors.TextMain),
        )
    }
}

@Composable
private fun RulesLink(onRulesClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .then(if (onRulesClick != null) Modifier.clickable(onClick = onRulesClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = null,
            tint = TrendColors.TextSub,
            modifier = Modifier.size(16.dp),
        )
        Text("القواعد", color = TrendColors.TextSub, fontSize = 13.sp)
    }
}

@Composable
private fun Podium(
    topUsers: List<TrendUiUser>,
    avatar: @Composable (TrendUiUser, Modifier) -> Unit,
    onUserClick: (TrendUiUser) -> Unit,
) {
    val first = topUsers.getOrNull(0)
    val second = topUsers.getOrNull(1)
    val third = topUsers.getOrNull(2)

    // في RTL: أول عنصر يظهر يميناً. الثاني يميناً، الأول بالوسط، الثالث يساراً.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumSlot(
            user = second, rank = 2,
            top = TrendColors.SilverTop, bottom = TrendColors.SilverBottom, ring = TrendColors.SilverRing,
            cardHeight = 176.dp, avatarSize = 76.dp,
            avatar = avatar, onUserClick = onUserClick,
            modifier = Modifier.weight(1f),
        )
        PodiumSlot(
            user = first, rank = 1,
            top = TrendColors.GoldTop, bottom = TrendColors.GoldBottom, ring = TrendColors.GoldRing,
            cardHeight = 204.dp, avatarSize = 96.dp,
            avatar = avatar, onUserClick = onUserClick,
            modifier = Modifier.weight(1.15f),
        )
        PodiumSlot(
            user = third, rank = 3,
            top = TrendColors.BronzeTop, bottom = TrendColors.BronzeBottom, ring = TrendColors.BronzeRing,
            cardHeight = 176.dp, avatarSize = 76.dp,
            avatar = avatar, onUserClick = onUserClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PodiumSlot(
    user: TrendUiUser?,
    rank: Int,
    top: Color,
    bottom: Color,
    ring: Color,
    cardHeight: Dp,
    avatarSize: Dp,
    avatar: @Composable (TrendUiUser, Modifier) -> Unit,
    onUserClick: (TrendUiUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (user == null) {
        Spacer(modifier.height(cardHeight))
        return
    }
    Box(modifier.height(cardHeight + 20.dp), contentAlignment = Alignment.TopCenter) {
        // البطاقة
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(cardHeight)
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(top, bottom)))
                .clickable { onUserClick(user) }
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp)) // مساحة للميدالية
            avatar(
                user,
                Modifier
                    .size(avatarSize)
                    .border(BorderStroke(3.dp, ring), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = user.name,
                color = TrendColors.TextMain,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            ScoreText(user.scoreText, 14.sp)
        }
        // شارة المركز فوق البطاقة
        MedalBadge(rank = rank, ring = ring, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun MedalBadge(rank: Int, ring: Color, modifier: Modifier = Modifier) {
    val size = if (rank == 1) 44.dp else 38.dp
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, TrendMedalShape)
            .clip(TrendMedalShape)
            .background(Brush.verticalGradient(listOf(ring, ring.copy(alpha = 0.72f))))
            .border(2.dp, Color.White.copy(alpha = 0.75f), TrendMedalShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            color = Color.White,
            fontSize = if (rank == 1) 22.sp else 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun RankRow(
    user: TrendUiUser,
    avatar: @Composable (TrendUiUser, Modifier) -> Unit,
    onUserClick: (TrendUiUser) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick(user) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // أقصى اليمين: رقم الترتيب
        Text(
            text = user.rankLabel,
            color = TrendColors.TextSub,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(30.dp),
        )
        Spacer(Modifier.width(10.dp))
        avatar(user, Modifier.size(52.dp).clip(CircleShape))
        Spacer(Modifier.width(12.dp))
        // الاسم: يأخذ المساحة المتبقية ويُقتطع إن كان طويلاً دون أن يتداخل مع النقاط
        Text(
            text = user.name,
            color = TrendColors.TextMain,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        ScoreText(user.scoreText, 15.sp)
    }
}

@Composable
private fun CurrentUserBar(
    user: TrendUiUser,
    avatar: @Composable (TrendUiUser, Modifier) -> Unit,
    onUserClick: (TrendUiUser) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUserClick(user) }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = user.rankLabel,
                color = TrendColors.TextSub,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp),
            )
            Spacer(Modifier.width(6.dp))
            avatar(user, Modifier.size(52.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(
                text = user.name,
                color = TrendColors.TextMain,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            ScoreText(user.scoreText, 15.sp)
        }
    }
}

@Composable
private fun ScoreText(text: String, size: androidx.compose.ui.unit.TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "🔥", fontSize = size)
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = TrendColors.TextMain, fontSize = size)
    }
}