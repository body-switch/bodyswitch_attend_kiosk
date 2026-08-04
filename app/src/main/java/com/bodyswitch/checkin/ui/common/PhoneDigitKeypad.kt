package com.bodyswitch.checkin.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 출입등록 디자인 토큰 (design_handoff_access_registration/Kiosk.dc.html)
private val Teal = Color(0xFF45B6B0)
private val OnTeal = Color(0xFF062B2A)
private val CellBg = Color(0xFF12161A)
private val CellBorder = Color(0xFF2A333A)
private val KeyLight = Color(0xFFE7EAE9)
private val KeyText = Color(0xFF15181C)

private const val CLEAR_KEY = "C"
private const val BACKSPACE_KEY = "⌫"

/**
 * 번호 입력 영역의 최대 폭. 키 118dp × 3 + 간격 14dp × 2 기준이라, 이 폭 이상이면
 * 체크인과 출입등록의 키패드가 픽셀 단위로 같아진다.
 */
val PHONE_KEYPAD_MAX_WIDTH = 382.dp

/**
 * 전화번호 뒤 4자리 입력 칸.
 *
 * 체크인(MainCheckinScreen)과 출입등록(AccessRegistrationScreen)이 같은 UI를 쓴다.
 */
@Composable
fun PhoneDigitCells(
    digits: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = PHONE_KEYPAD_MAX_WIDTH),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val char = digits.getOrNull(i)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CellBg)
                    .border(
                        3.dp,
                        if (char != null) Teal else CellBorder,
                        RoundedCornerShape(18.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = char?.toString() ?: "",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Teal,
                )
            }
        }
    }
}

/**
 * 숫자 키패드 (3 × 4, 마지막 줄은 C / 0 / 백스페이스).
 *
 * 체크인과 출입등록이 같은 UI를 쓴다.
 */
@Composable
fun PhoneKeypad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keypadRows = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(CLEAR_KEY, "0", BACKSPACE_KEY),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = PHONE_KEYPAD_MAX_WIDTH),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                row.forEach { key ->
                    val isAction = key == CLEAR_KEY || key == BACKSPACE_KEY
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isAction) Teal else KeyLight)
                            .clickable {
                                when (key) {
                                    CLEAR_KEY -> onClear()
                                    BACKSPACE_KEY -> onDelete()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == BACKSPACE_KEY) {
                            Icon(
                                Icons.Default.Backspace,
                                contentDescription = "지우기",
                                modifier = Modifier.size(30.dp),
                                tint = OnTeal,
                            )
                        } else {
                            Text(
                                text = key,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAction) OnTeal else KeyText,
                            )
                        }
                    }
                }
            }
        }
    }
}
