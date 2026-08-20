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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
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
// 퇴실은 되돌리기 어려운 동작이라 붉은 계열로 구분하되, 경고처럼 보이지 않게 톤을 낮춘다.
private val ChoiceExit = Color(0xFFE57373)
private val ChoiceCardBg = Color(0xFF262626)
private val ChoiceBackText = Color(0xFF9E9E9E)

/**
 * 당일 미마감 입장 기록이 있는 회원에게 계속 이용할지 퇴실할지 고르게 하는 화면.
 *
 * 첫 입장 회원은 퇴실할 대상이 없으므로 이 화면을 거치지 않고 기존 이용권 선택 흐름으로 간다.
 *
 * 왼쪽 버튼은 무차감 재입장 자격이 있으면 바로 재입장하고, 없으면 이용권 선택(차감) 흐름으로 간다.
 * 그 분기는 호출부(onContinue)가 처리하며 회원에게는 "재입장" 하나로 보인다.
 */
@Composable
fun MemberAttendTypeScreen(
    memberName: String,
    onContinue: () -> Unit,
    onCheckout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 회원 사진 자리. 서버가 프로필 이미지를 내려주지 않아 실루엣으로 대신한다.
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(ChoiceCardBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = ChoicePrimary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = memberName,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = ChoicePrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ChoiceCard(
                // 무차감 재입장이든 이용권 선택(차감)이든 회원에게는 "재입장"으로 통일해 보여준다.
                label = "재입장",
                icon = Icons.AutoMirrored.Filled.Login,
                accent = ChoicePrimary,
                onClick = onContinue,
                modifier = Modifier.weight(1f),
            )
            ChoiceCard(
                label = "퇴실",
                icon = Icons.AutoMirrored.Filled.Logout,
                accent = ChoiceExit,
                onClick = onCheckout,
                modifier = Modifier.weight(1f),
            )
        }
    }

        // 뒤로가기. 가운데 정렬된 본문을 밀지 않도록 겹쳐 놓는다.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 48.dp, top = 12.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onBack() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                tint = ChoiceBackText,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "뒤로가기",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = ChoiceBackText,
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        // 고정 높이를 주면 아이콘+라벨이 넘쳐 글자가 잘린다. 두 카드 구조가 같아 내용 기준으로도 높이가 맞는다.
        modifier = modifier
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
                .size(112.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = label,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
