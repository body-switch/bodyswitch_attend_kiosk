package com.bodyswitch.checkin.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.bodyswitch.checkin.R
import com.bodyswitch.checkin.ui.common.isPortrait
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val TealPrimary = Color(0xFF4AB3BC)
private val DarkBg = Color(0xFF000000)
private val PanelBg = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
private val GrayBorder = Color(0xFFA6A6A6)
private val Red = Color(0xFFE53935)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { focusManager.clearFocus() },
    ) {
        if (isPortrait()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 상단 - 로고 + 배경 이미지 (세로에서는 고정 높이로 축소)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.bg_diamond),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Image(
                        painter = painterResource(R.drawable.logo_bodyswitch),
                        contentDescription = "BODYSWITCH",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(horizontal = 24.dp),
                        contentScale = ContentScale.FillWidth,
                    )
                }

                // 하단 - 로그인 폼 패널
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(PanelBg)
                        .imePadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 48.dp, vertical = 32.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LoginFormContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            focusManager = focusManager,
                        )
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // 왼쪽 - 로고 + 배경 이미지
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.bg_diamond),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Image(
                        painter = painterResource(R.drawable.logo_bodyswitch),
                        contentDescription = "BODYSWITCH",
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(horizontal = 24.dp),
                        contentScale = ContentScale.FillWidth,
                    )
                }

                // 오른쪽 - 로그인 폼 패널
                Box(
                    modifier = Modifier
                        .width(480.dp)
                        .fillMaxHeight()
                        .background(PanelBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LoginFormContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            focusManager = focusManager,
                        )
                    }
                }
            }
        }

        // 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Red,
                contentColor = Color.White,
            )
        }
    }
}

@Composable
private fun LoginFormContent(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: FocusManager,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    // 타이틀
    Text(
        text = "로그인",
        fontSize = 48.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
    )

    Spacer(modifier = Modifier.height(24.dp))

    HorizontalDivider(color = GrayBorder, thickness = 1.dp)

    Spacer(modifier = Modifier.height(32.dp))

    // 아이디 필드
    Text(
        text = "아이디",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
    )

    Spacer(modifier = Modifier.height(8.dp))

    TextField(
        value = uiState.username,
        onValueChange = viewModel::onUsernameChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "아이디를 입력해 주세요",
                color = GrayBorder,
                fontSize = 16.sp,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            cursorColor = TealPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 비밀번호 필드
    Text(
        text = "비밀번호",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
    )

    Spacer(modifier = Modifier.height(8.dp))

    TextField(
        value = uiState.password,
        onValueChange = viewModel::onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "비밀번호를 입력해 주세요",
                color = GrayBorder,
                fontSize = 16.sp,
            )
        },
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Filled.Visibility
                    } else {
                        Icons.Filled.VisibilityOff
                    },
                    contentDescription = if (passwordVisible) {
                        "비밀번호 숨기기"
                    } else {
                        "비밀번호 표시"
                    },
                    tint = GrayBorder,
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                viewModel.login()
            }
        ),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            cursorColor = TealPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 자동로그인 체크박스
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { viewModel.onAutoLoginChange(!uiState.autoLogin) },
    ) {
        Checkbox(
            checked = uiState.autoLogin,
            onCheckedChange = viewModel::onAutoLoginChange,
            modifier = Modifier.size(24.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = TealPrimary,
                uncheckedColor = Color.White,
                checkmarkColor = Color.White,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "자동로그인",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 로그인 버튼
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = TealPrimary)
        }
    } else {
        Button(
            onClick = { viewModel.login() },
            enabled = uiState.username.isNotBlank() && uiState.password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealPrimary,
                disabledContainerColor = TealPrimary.copy(alpha = 0.4f),
            ),
        ) {
            Text(
                text = "로그인",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Preview(
    name = "로그인 화면",
    widthDp = 1920,
    heightDp = 1080,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(
        onLoginSuccess = {},
    )
}
