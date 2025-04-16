package com.example.captive_portal_analyzer_kotlin.screens.setup_pcapdroid

import android.app.Application
import android.content.Intent
import android.widget.VideoView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.captive_portal_analyzer_kotlin.R
import com.example.captive_portal_analyzer_kotlin.components.RoundCornerButton
//todo make sure target apps step is not needed and remove from setup flow
@Composable
fun SetupPCAPDroidScreen(
    navigateToManualConnectScreen: () -> Unit,
    updateSkipSetup: (Boolean) -> Unit,
    viewModel: SetupPCAPDroidViewModel = viewModel(
        factory = SetupPCAPDroidViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val isPcapDroidInstalled = viewModel.isPcapDroidInstalled.collectAsState().value
    val currentStep = viewModel.currentStep.collectAsState().value
    val skipSetup = viewModel.skipSetup.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.refreshPcapDroidStatus()
    }

    SetupPCAPDroidScreenContent(
        currentStep = currentStep,
        isPcapDroidInstalled = isPcapDroidInstalled,
        skipSetup = skipSetup,
        navigateToManualConnectScreen = navigateToManualConnectScreen,
        onStepChange = { step -> viewModel.setStep(step) },
        onSkipSetupChange = { value ->
            updateSkipSetup(value) //to update the shared preferences
            viewModel.setSkipSetup(value) //to update ui
        }
    )
}

@Composable
private fun SetupPCAPDroidScreenContent(
    currentStep: Int,
    isPcapDroidInstalled: Boolean,
    skipSetup: Boolean,
    navigateToManualConnectScreen: () -> Unit,
    onStepChange: (Int) -> Unit,
    onSkipSetupChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    val steps = listOf(
        Step(
            title = stringResource(R.string.setup_pcapdroid),
            content = { _, _ ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.app_uses_pcapdroid),
                        style = typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    val infiniteTransition = rememberInfiniteTransition()
                    val offsetX = infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -20f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.swipe_left_24px),
                        contentDescription = stringResource(R.string.swipe_left_to_continue),
                        modifier = Modifier
                            .size(48.dp)
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally)
                            .offset(x = offsetX.value.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.skip_pcapdroid_setup),
                        style = typography.labelLarge,
                        color = colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { navigateToManualConnectScreen() }
                    )
                }
            }
        ),
        Step(
            title = stringResource(R.string.step_1_install_pcapdroid),
            content = { isCompleted, _ ->
                if (isCompleted) {
                    Step1Content(isInstalled = true, onInstallClick = {})
                } else {
                    Step1Content(
                        isInstalled = false,
                        onInstallClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = "market://details?id=com.emanuelef.remote_capture".toUri()
                                setPackage("com.android.vending")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        ),
        Step(
            title = stringResource(R.string.step_2_enable_tls_decryption),
            content = { _, _ ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            val text =
                                stringResource(R.string.in_pcapdroid_settings_top_right_icon_toggle_tls_decryption_switch_under_traffic_inspection_menu)
                            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                            var lastIndex = 0

                            boldRegex.findAll(text).forEach { matchResult ->
                                append(text.substring(lastIndex, matchResult.range.first))
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(matchResult.groupValues[1])
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            if (lastIndex < text.length) {
                                append(text.substring(lastIndex))
                            }
                        },
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center

                    )

                    Text(
                        text = stringResource(R.string.follow_the_pcapdroid_instructions_to_install_the_mitm_addon_and_the_ca_certificate_on_your_device),
                        style = typography.bodyMedium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        ),
        Step(
            title = stringResource(R.string.step_3_add_decryption_rules),
            content = { _, _ ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            val text =
                                stringResource(R.string.from_app_menu_select_decryption_rules_then_click_the_icon_then_select_app_then_add_the_two_apps_captive_portal_analyzer_captive_portal_login)
                            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                            var lastIndex = 0

                            boldRegex.findAll(text).forEach { matchResult ->
                                append(text.substring(lastIndex, matchResult.range.first))
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(matchResult.groupValues[1])
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            if (lastIndex < text.length) {
                                append(text.substring(lastIndex))
                            }
                        },
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )

                    LoopingVideoPlayer(
                        videoResId = R.raw.decryption_rules,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }
            }
        ),
        Step(
            title = stringResource(R.string.step_4_select_target_apps),
            content = { _, _ ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            val text =
                                stringResource(R.string.from_target_apps_on_main_screen_please_select_captive_portal_analyzer_captive_portal_login)
                            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                            var lastIndex = 0

                            boldRegex.findAll(text).forEach { matchResult ->
                                append(text.substring(lastIndex, matchResult.range.first))
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(matchResult.groupValues[1])
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            if (lastIndex < text.length) {
                                append(text.substring(lastIndex))
                            }
                        },
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )

                    LoopingVideoPlayer(
                        videoResId = R.raw.target_apps,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }
            }
        ),
        Step(
            title = stringResource(R.string.step_5_done),
            content = { _, _ ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.great_you_are_all_set_to_start_capturing_packets),
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    )

                    Text(
                        text =
                            stringResource(R.string.no_need_to_open_pcapdroid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // don't show again checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Checkbox(
                            checked = skipSetup,
                            onCheckedChange = { onSkipSetupChange(it) }
                        )
                        Text(
                            text = "Don't show this setup again",
                            style = typography.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    RoundCornerButton(
                        onClick = navigateToManualConnectScreen,
                        buttonText = stringResource(R.string.continuee),
                        trailingIcon = painterResource(id = R.drawable.arrow_forward_ios_24px)
                    )
                }
            }
        )
    )

    val pagerState = rememberPagerState(
        initialPage = currentStep,
        pageCount = { steps.size }
    )

    LaunchedEffect(currentStep) {
        pagerState.scrollToPage(currentStep)
    }

    LaunchedEffect(pagerState.currentPage) {
        onStepChange(pagerState.currentPage)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    steps[page].content(isPcapDroidInstalled) {}
                }
            }

            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(steps.size) { iteration ->
                    val color =
                        if (currentStep == iteration) colors.primary else colors.onSurface.copy(
                            alpha = 0.5f
                        )
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }
    }
}

data class Step(
    val title: String,
    val content: @Composable (isCompleted: Boolean, onNext: () -> Unit) -> Unit
)

@Composable
fun LoopingVideoPlayer(
    videoResId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoView = remember {
        VideoView(context).apply {
            setVideoPath("android.resource://${context.packageName}/$videoResId")
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.start()
            }
        }
    }

    AndroidView(
        factory = { videoView },
        modifier = modifier.fillMaxWidth(),
        update = { view ->
            if (!view.isPlaying) {
                view.start()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
        }
    }
}

@Composable
private fun Step1Content(
    isInstalled: Boolean,
    onInstallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.step_1_install_pcapdroid),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = if (isInstalled) {
                stringResource(R.string.pcapdroid_is_installed_on_your_device)
            } else {
                stringResource(R.string.pcapdroid_is_required_to_capture_network_traffic_please_install_it_from_google_play)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (!isInstalled) {
            RoundCornerButton(
                onClick = onInstallClick,
                buttonText = stringResource(R.string.install_pcapdroid),
                trailingIcon = painterResource(id = R.drawable.apk_install_24px),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(name = "Step 0 - Intro", showBackground = true)
@Composable
fun PreviewStep0() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            currentStep = 0,
            isPcapDroidInstalled = false,
            navigateToManualConnectScreen = {},
            onStepChange = {},
            skipSetup = false,
            onSkipSetupChange ={},
        )
    }
}

@Preview(name = "Step 1 - Install", showBackground = true)
@Composable
fun PreviewStep1NotInstalled() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            currentStep = 1,
            isPcapDroidInstalled = false,
            navigateToManualConnectScreen = {},
            onStepChange = {},
            skipSetup = false,
            onSkipSetupChange ={},
        )
    }
}

@Preview(name = "Step 1 - Installed", showBackground = true)
@Composable
fun PreviewStep1Installed() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            currentStep = 1,
            isPcapDroidInstalled = true,
            navigateToManualConnectScreen = {},
            onStepChange = {},
            skipSetup = false,
            onSkipSetupChange ={},
        )
    }
}

@Preview(name = "Step 2 - TLS Decryption", showBackground = true)
@Composable
fun PreviewStep2() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            currentStep = 2,
            isPcapDroidInstalled = true,
            navigateToManualConnectScreen = {},
            onStepChange = {},
            skipSetup = false,
            onSkipSetupChange ={},
        )
    }
}

@Preview(name = "Step 3 - Decryption Rules", showBackground = true)
@Composable
fun PreviewStep3() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            currentStep = 3,
            isPcapDroidInstalled = true,
            navigateToManualConnectScreen = {},
            onStepChange = {},
            skipSetup = false,
            onSkipSetupChange ={},
        )
    }
}

@Preview(name = "Step 4 - Target Apps", showBackground = true)
@Composable
fun PreviewStep4() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            currentStep = 4,
            isPcapDroidInstalled = true,
            navigateToManualConnectScreen = {},
            onStepChange = {},
            skipSetup = false,
            onSkipSetupChange ={},
        )
    }
}

@Preview(name = "Step 5 - Done", showBackground = true)
@Composable
fun PreviewStep5() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            currentStep = 5,
            isPcapDroidInstalled = true,
            navigateToManualConnectScreen = {},
            onStepChange = {},
            skipSetup = false,
            onSkipSetupChange ={},
        )
    }
}