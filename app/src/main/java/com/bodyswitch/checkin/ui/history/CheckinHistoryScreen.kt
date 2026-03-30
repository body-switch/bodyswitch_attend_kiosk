package com.bodyswitch.checkin.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodyswitch.checkin.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DarkBg = Color(0xFF1A1A1A)
private val TealPrimary = Color(0xFF4AB3BC)
private val GreenActive = Color(0xFF55C982)
private val GrayText = Color(0xFFA6A6A6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinHistoryScreen(
    onBack: () -> Unit,
    viewModel: CheckinHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // DatePicker dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.dismissDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.selectDate(date)
                    }
                }) {
                    Text("확인", color = TealPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDatePicker() }) {
                    Text("취소", color = GrayText)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { focusManager.clearFocus() },
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 뒤로가기
            Row(
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "뒤로가기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }

            // 날짜 드롭다운 (가운데 정렬)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clickable { viewModel.toggleDatePicker() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.selectedDate.format(dateFormatter),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))

            // 검색바 (흰색 테두리, 배경 투명)
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .width(360.dp)
                    .height(56.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
                placeholder = {
                    Text("회원명 또는 전화번호 검색", color = GrayText, fontSize = 14.sp)
                },
                trailingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "검색",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                focusManager.clearFocus()
                                viewModel.search()
                            },
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.search()
                    },
                ),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TealPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }

        // 리스트
        when {
            uiState.isLoading && uiState.records.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(uiState.error ?: "", color = Color(0xFFEF5350), fontSize = 16.sp)
                }
            }
            uiState.records.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("출입 기록이 없습니다", color = GrayText, fontSize = 16.sp)
                }
            }
            else -> {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        lastVisible >= uiState.records.size - 3
                    }
                }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) viewModel.loadMore()
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(uiState.records, key = { it.id }) { record ->
                        AttendanceRow(record = record)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    }
                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = TealPrimary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(record: AttendanceRecord) {
    val phoneLast4 = record.phoneNumber?.takeLast(4) ?: ""
    val nameDisplay = if (phoneLast4.isNotEmpty()) {
        "${record.memberName} ($phoneLast4)"
    } else {
        record.memberName
    }

    val statusColor = if (record.status == "이용중") GreenActive else GrayText
    val isPass = record.ticketName?.contains("이용권") == true || record.usageCount == null
    val usageText = if (!isPass && record.usageCount != null && record.remainCount != null) {
        "${record.remainCount} / ${record.usageCount} (${record.remainCount}회 남음)"
    } else {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 왼쪽: 회원 정보
        Column(modifier = Modifier.weight(1f)) {
            // 이름 (전번뒤4)
            Text(
                text = nameDisplay,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 지점명
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_door),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = record.branchName,
                    fontSize = 13.sp,
                    color = GrayText,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 시간
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${record.date} ${record.startTime}",
                    fontSize = 13.sp,
                    color = GrayText,
                )
            }
        }

        // 오른쪽: 상태 + 회차 + 이용권
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            // 이용중 / 퇴실
            Text(
                text = record.status,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
            // N회차
            Text(
                text = "${record.attendCount}회차",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
            )
            // 이용권 잔여
            if (usageText.isNotEmpty()) {
                Text(
                    text = usageText,
                    fontSize = 12.sp,
                    color = GrayText,
                )
            }
        }
    }
}
