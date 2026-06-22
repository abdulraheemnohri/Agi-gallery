package com.google.ai.edge.gallery.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.customtasks.tinygarden.TinyGardenContent
import com.google.ai.edge.gallery.customtasks.tinygarden.TinyGardenViewModel
import com.google.ai.edge.gallery.customtasks.mobileactions.MobileActionsContent
import com.google.ai.edge.gallery.customtasks.mobileactions.MobileActionsViewModel
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.common.chat.ChatViewContent
import com.google.ai.edge.gallery.ui.common.chat.ChatViewModel
import com.google.ai.edge.gallery.ui.llmchat.LlmAskAudioViewModel
import com.google.ai.edge.gallery.ui.llmchat.LlmAskImageViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.ui.common.textandvoiceinput.TextAndVoiceInput
import com.google.ai.edge.gallery.ui.common.textandvoiceinput.HoldToDictateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modelManagerViewModel: ModelManagerViewModel,
    onNavigateUp: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val coroutineScope = rememberCoroutineScope()
    val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
    val selectedModel = modelManagerUiState.selectedModel

    val tabs = listOf("Tiny Garden", "Mobile Actions", "Audio Scribe", "Ask Image", "AI Chat", "Agent Skills")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Google AI Edge Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Model: ${selectedModel.name} | Running on GPU",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(32.dp))
                    }
                }
            )
        },
        bottomBar = {
            val holdToDictateViewModel: HoldToDictateViewModel = hiltViewModel()
            Surface(tonalElevation = 8.dp) {
                TextAndVoiceInput(
                    task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT)!!,
                    processing = false,
                    holdToDictateViewModel = holdToDictateViewModel,
                    onDone = { },
                    onAmplitudeChanged = { },
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        val viewModel: TinyGardenViewModel = hiltViewModel()
                        TinyGardenContent(viewModel = viewModel, snackbarHostState = remember { SnackbarHostState() })
                    }
                    1 -> {
                        val viewModel: MobileActionsViewModel = hiltViewModel()
                        MobileActionsContent(
                            task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_MOBILE_ACTIONS)!!,
                            model = selectedModel,
                            viewModel = viewModel,
                            curActions = remember { mutableStateListOf() },
                            tools = emptyList(),
                            snackbarHostState = remember { SnackbarHostState() },
                            onProcessingStarted = {}
                        )
                    }
                    2 -> {
                        val viewModel: LlmAskAudioViewModel = hiltViewModel()
                        ChatViewContent(
                            task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_AUDIO)!!,
                            selectedModel = selectedModel,
                            viewModel = viewModel,
                            modelManagerViewModel = modelManagerViewModel,
                            onSendMessage = { _, _ -> },
                            onRunAgainClicked = { _, _ -> },
                            onBenchmarkClicked = { _, _, _, _ -> },
                            onStopButtonClicked = {},
                            onSkillClicked = {},
                            onMcpClicked = {},
                            showAudioPicker = true
                        )
                    }
                    3 -> {
                        val viewModel: LlmAskImageViewModel = hiltViewModel()
                        ChatViewContent(
                            task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_IMAGE)!!,
                            selectedModel = selectedModel,
                            viewModel = viewModel,
                            modelManagerViewModel = modelManagerViewModel,
                            onSendMessage = { _, _ -> },
                            onRunAgainClicked = { _, _ -> },
                            onBenchmarkClicked = { _, _, _, _ -> },
                            onStopButtonClicked = {},
                            onSkillClicked = {},
                            onMcpClicked = {},
                            showImagePicker = true
                        )
                    }
                    4 -> {
                        val viewModel: LlmChatViewModel = hiltViewModel()
                        ChatViewContent(
                            task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT)!!,
                            selectedModel = selectedModel,
                            viewModel = viewModel,
                            modelManagerViewModel = modelManagerViewModel,
                            onSendMessage = { _, _ -> },
                            onRunAgainClicked = { _, _ -> },
                            onBenchmarkClicked = { _, _, _, _ -> },
                            onStopButtonClicked = {},
                            onSkillClicked = {},
                            onMcpClicked = {}
                        )
                    }
                    5 -> {
                        val viewModel: LlmChatViewModel = hiltViewModel()
                        ChatViewContent(
                            task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_AGENT_CHAT)!!,
                            selectedModel = selectedModel,
                            viewModel = viewModel,
                            modelManagerViewModel = modelManagerViewModel,
                            onSendMessage = { _, _ -> },
                            onRunAgainClicked = { _, _ -> },
                            onBenchmarkClicked = { _, _, _, _ -> },
                            onStopButtonClicked = {},
                            onSkillClicked = {},
                            onMcpClicked = {},
                            skillCount = 3,
                            mcpCount = 1
                        )
                    }
                }
            }
        }
    }
}
