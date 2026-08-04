package com.bodyswitch.checkin.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyswitch.checkin.data.api.dto.MemberCandidate

private val Teal = Color(0xFF45B6B0)
private val TextPrimary = Color(0xFFF4F6F5)
private val TextMuted = Color(0xFF8A9299)
private val CardBg = Color(0xFF1C2226)
private val CardBorder = Color(0xFF2A333A)
private val AvatarIcon = Color(0xFF8B949B)
private val BadgeValid = Color(0xFF22C55E)
private val BadgeStopped = Color(0xFFF4CE00)
private val BadgeExpired = Color(0xFFE53935)
private val BadgeEmployee = Color(0xFF7DA7FF)

private const val ROLE_EMPLOYEE = "EMPLOYEE"
private const val PASS_VALID = "VALID"
private const val PASS_STOPPED = "STOPPED"

/**
 * 뒤 4자리가 겹칠 때 본인을 고르는 목록.
 *
 * 뒤 4자리는 완전일치보다 훨씬 자주 겹쳐서(운영 기준 43%) 이름만으로는 못 고른다.
 * 생년은 가리고 월일만, 전화번호는 앞자리만 함께 보여준다. 이용권 상태를 같이 띄워
 * 만료 회원도 자기 항목을 찾아 만료 안내까지 갈 수 있게 한다.
 *
 * 후보 수에 상한을 두지 않는다. 더미번호를 쓰는 지점은 수십 명이 나올 수 있어
 * 호출하는 쪽에서 스크롤 가능한 영역에 넣는다.
 */
@Composable
fun MemberCandidateList(
    candidates: List<MemberCandidate>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        candidates.forEach { candidate ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg)
                    .border(1.5.dp, CardBorder, RoundedCornerShape(24.dp))
                    .clickable { onSelect(candidate.candidateId) }
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = AvatarIcon,
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            candidate.name,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                        )
                        CandidateBadge(candidate)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "생년월일 ${candidate.maskedBirthDate}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Teal,
                    )
                    candidate.maskedPhone?.let { phone ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            phone,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateBadge(candidate: MemberCandidate) {
    val (label, color) = when {
        candidate.role == ROLE_EMPLOYEE -> "직원" to BadgeEmployee
        candidate.passStatus == PASS_VALID -> "이용중" to BadgeValid
        candidate.passStatus == PASS_STOPPED -> "중지" to BadgeStopped
        candidate.passStatus != null -> "만료" to BadgeExpired
        else -> return
    }

    Spacer(modifier = Modifier.width(14.dp))
    Text(
        label,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
