package com.bodyswitch.checkin.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ChoicePrimary = Color(0xFF4AB3BC)
private val ChoiceExit = Color(0xFF737373)
private val ChoiceCardBg = Color(0xFF262626)
private val ChoiceTextMuted = Color(0xFFA6A6A6)

/**
 * 당일 미마감 입장 기록이 있는 회원에게 계속 이용할지 퇴실할지 고르게 하는 화면.
 *
 * 첫 입장 회원은 퇴실할 대상이 없으므로 이 화면을 거치지 않고 기존 이용권 선택 흐름으로 간다.
 *
 * @param canReentry 무차감 재입장 자격. 없으면 왼쪽 버튼이 기존 이용권 선택 흐름으로 간다.
 */
@Composable
fun MemberAttendTypeScreen(
    memberName: String,
    canReentry: Boolean,
    onContinue: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = memberName,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = ChoicePrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "오늘 이용 중이십니다.\n어떤 처리를 하시겠어요?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = ChoiceTextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(56.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ChoiceCard(
                label = if (canReentry) "재입장" else "계속 이용",
                description = if (canReentry) "차감 없이 다시 입장" else "이용권을 선택합니다",
                icon = Icons.AutoMirrored.Filled.Login,
                accent = ChoicePrimary,
                onClick = onContinue,
                modifier = Modifier.weight(1f),
            )
            ChoiceCard(
                label = "퇴실",
                description = "오늘 이용을 마칩니다",
                icon = Icons.AutoMirrored.Filled.Logout,
                accent = ChoiceExit,
                onClick = onCheckout,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    label: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(360.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ChoiceCardBg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onClick() }
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = label,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = ChoiceTextMuted,
            textAlign = TextAlign.Center,
        )
    }
}
