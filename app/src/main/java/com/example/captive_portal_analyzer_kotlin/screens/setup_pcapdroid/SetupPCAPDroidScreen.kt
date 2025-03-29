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

@Composable
fun SetupPCAPDroidScreen(
    navigateToManualConnectScreen: () -> Unit,
    viewModel: SetupPCAPDroidViewModel = viewModel(
        factory = SetupPCAPDroidViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val isPcapDroidInstalled = viewModel.isPcapDroidInstalled.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.refreshPcapDroidStatus()
    }

    SetupPCAPDroidScreenContent(
        isPcapDroidInstalled = isPcapDroidInstalled,
        navigateToManualConnectScreen = navigateToManualConnectScreen
    )
}

@Composable
private fun SetupPCAPDroidScreenContent(
    isPcapDroidInstalled: Boolean,
    navigateToManualConnectScreen: () -> Unit
) {
    val context = LocalContext.current
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    val steps = listOf(


        Step(
            title = "Install PCAPDroid",
            content = { isCompleted, _ ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.app_uses_pcapdroid),
                        style = typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    // Animated swipe-left icon
                    val infiniteTransition = rememberInfiniteTransition()
                    val offsetX = infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -20f, // Move left by 20dp
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000, // 1 second per cycle
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse // Move back and forth
                        )
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.swipe_left_24px),
                        contentDescription = "Swipe left to continue",
                        modifier = Modifier
                            .size(48.dp) // Adjust size as needed
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally)
                            .offset(x = offsetX.value.dp) // Apply the animated offset
                    )

                    Spacer(modifier = Modifier.weight(1f))

               Text(
                   text = "Skip PCAPDroid Setup",
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
            title = "Step 1: Install PCAPDroid",
            content = { isCompleted, _ -> // Removed onNext parameter usage
                if (isPcapDroidInstalled) {
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
            title = "Step 2: Enable TLS Decryption",
            content = { _, _ -> // Removed onNext parameter usage

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
                                "In PCAPdroid settings (top right ⚙️ icon), toggle **TLS decryption** switch under **Traffic inspection** menu."
                            val boldRegex =
                                Regex("\\*\\*(.*?)\\*\\*") // Matches text between ** and **
                            var lastIndex = 0

                            boldRegex.findAll(text).forEach { matchResult ->
                                // Append text before the bold section
                                append(text.substring(lastIndex, matchResult.range.first))
                                // Append the bold section
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(matchResult.groupValues[1]) // groupValues[1] is the text inside **
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            // Append any remaining text after the last bold section
                            if (lastIndex < text.length) {
                                append(text.substring(lastIndex))
                            }
                        },
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )

                    Text(
                        text = "Follow the PCAPDroid instructions to install the mitm addon and the CA certificate on your device.",
                        style = typography.bodyMedium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        ),
        Step(
            title = "Step 3: Add Decryption Rules",
            content = { _, _ -> // Removed onNext parameter usage

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
                                "From app menu select **Decryption rules**, then click the **+** icon then select **App** then add the two apps **Captive Portal Analyzer** & **Captive Portal Login**"
                            val boldRegex =
                                Regex("\\*\\*(.*?)\\*\\*") // Matches text between ** and **
                            var lastIndex = 0

                            boldRegex.findAll(text).forEach { matchResult ->
                                // Append text before the bold section
                                append(text.substring(lastIndex, matchResult.range.first))
                                // Append the bold section
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(matchResult.groupValues[1]) // groupValues[1] is the text inside **
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            // Append any remaining text after the last bold section
                            if (lastIndex < text.length) {
                                append(text.substring(lastIndex))
                            }
                        },
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(16.dp)
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
            title = "Step 4: Select Target Apps",
            content = { _, _ -> // Removed onNext parameter usage
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
                                "From **Target apps** on main screen please select **Captive Portal Analyzer** & **Captive Portal Login**"
                            val boldRegex =
                                Regex("\\*\\*(.*?)\\*\\*") // Matches text between ** and **
                            var lastIndex = 0

                            boldRegex.findAll(text).forEach { matchResult ->
                                // Append text before the bold section
                                append(text.substring(lastIndex, matchResult.range.first))
                                // Append the bold section
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(matchResult.groupValues[1]) // groupValues[1] is the text inside **
                                }
                                lastIndex = matchResult.range.last + 1
                            }
                            // Append any remaining text after the last bold section
                            if (lastIndex < text.length) {
                                append(text.substring(lastIndex))
                            }
                        },
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(16.dp)
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
            title = "Step 5: Done",
            content = { _, _ -> // Removed onNext parameter usage
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)

                ) {

                    Text(
                        text = "Great! You are all set to start capturing packets. 🎊",
                        style = typography.titleMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    )


                    RoundCornerButton(
                        onClick = navigateToManualConnectScreen,
                        buttonText = stringResource(R.string.continuee),
                        trailingIcon = painterResource(id = R.drawable.arrow_forward_ios_24px)
                    )
                }
            }
        )
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })

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

            // Page indicators
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(steps.size) { iteration ->
                    val color =
                        if (pagerState.currentPage == iteration) colors.primary else colors.onSurface.copy(
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
    videoResId: Int, // e.g., R.raw.my_video
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Create VideoView programmatically
    val videoView = remember {
        VideoView(context).apply {
            setVideoPath("android.resource://${context.packageName}/$videoResId")
            // Set up looping when the video is prepared
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.start()
            }
        }
    }

    // Use AndroidView to embed VideoView in Compose
    AndroidView(
        factory = { videoView },
        modifier = modifier.fillMaxWidth(),
        update = { view ->
            if (!view.isPlaying) {
                view.start()
            }
        }
    )

    // Cleanup when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
        }
    }
}

// Step 1 Content
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
            text = "Step 1: Install PCAPDroid",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = if (isInstalled) {
                "PCAPDroid is installed on your device."
            } else {
                "PCAPDroid is required to capture network traffic. Please install it from Google Play."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (!isInstalled) {
            RoundCornerButton(
                onClick =
                    onInstallClick,
                buttonText = stringResource(R.string.install_pcapdroid),
                trailingIcon = painterResource(id = R.drawable.apk_install_24px),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

        }
    }
}

//preview functions

@Preview(name = "NotInstalled", showBackground = true)
@Composable
fun SetupPCAPDroidScreenPreviewNotInstalled() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            navigateToManualConnectScreen = {}, // Empty lambda for preview
            isPcapDroidInstalled = false
        )
    }
}

@Preview(name = "NotInstalled", showBackground = true)
@Composable
fun SetupPCAPDroidScreenPreviewInstalled() {
    MaterialTheme {
        SetupPCAPDroidScreenContent(
            navigateToManualConnectScreen = {}, // Empty lambda for preview
            isPcapDroidInstalled = false
        )
    }
}

