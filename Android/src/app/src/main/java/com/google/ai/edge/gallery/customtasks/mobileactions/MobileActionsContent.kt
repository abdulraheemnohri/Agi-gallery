package com.google.ai.edge.gallery.customtasks.mobileactions

import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.GalleryEvent
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.firebaseAnalytics
import com.google.ai.edge.gallery.ui.common.MarkdownText
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning
import com.google.ai.edge.gallery.ui.common.chat.MessageBodyLoading
import com.google.ai.edge.gallery.ui.common.chat.MessageBodyWarning
import com.google.ai.edge.gallery.ui.common.getTaskBgGradientColors
import com.google.ai.edge.gallery.ui.common.getTaskIconColor
import com.google.ai.edge.litertlm.ToolProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AGMAContent"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileActionsContent(
    task: Task,
    model: Model,
    viewModel: MobileActionsViewModel,
    curActions: SnapshotStateList<Action>,
    tools: List<ToolProvider>,
    snackbarHostState: SnackbarHostState,
    onProcessingStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var doneGeneratingResponse by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val resources = LocalResources.current
    val taskColor = getTaskBgGradientColors(task = task)[1]

    val noFunctionCallSnackbarMessage = stringResource(R.string.snackbar_no_function_call)

    val send: (String) -> Unit = { text ->
        scope.launch(Dispatchers.Main) {
            selectedTabIndex = 0
            focusManager.clearFocus()
        }
        onProcessingStarted()
        doneGeneratingResponse = false
        viewModel.processUserPrompt(
            model = model,
            userPrompt = text,
            tools = tools,
            onProcessDone = {
                doneGeneratingResponse = true
                if (curActions.isNotEmpty()) {
                    val errors = mutableListOf<String>()
                    for (action in curActions) {
                        val curError = viewModel.performAction(action = action, context = context)
                        if (curError.isEmpty()) {
                            viewModel.addFunctionCallDetails(
                                details = genFormattedFunctionCall(action = action, resources = resources)
                            )
                        } else {
                            errors.add(curError)
                        }
                    }
                    if (errors.isNotEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                errors.joinToString(separator = "; "),
                                withDismissAction = true,
                                duration = SnackbarDuration.Long,
                            )
                        }
                    }
                } else {
                    viewModel.setNoFunctionRecognized(value = true)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            noFunctionCallSnackbarMessage,
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                        )
                    }
                }
            },
            onError = { error ->
                doneGeneratingResponse = true
                scope.launch {
                  snackbarHostState.showSnackbar(error)
                }
            },
        )
        firebaseAnalytics?.logEvent(
            GalleryEvent.GENERATE_ACTION.id,
            Bundle().apply {
                putString("capability_name", task.id)
                putString("model_id", model.name)
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.showWelcomeMessage) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.mobile_actions_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = getTaskIconColor(task = task),
                        )
                        Text(
                            stringResource(R.string.mobile_actions_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = getTaskIconColor(task = task),
                        )
                        Column {
                            Text(
                                stringResource(R.string.mobile_actions_supported_actions),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp).graphicsLayer { alpha = 0.7f },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            SAMPLE_ACTION_ITEMS.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        item.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        stringResource(item.labelResId),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        uiState.userPrompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }

                if (uiState.processing) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        MessageBodyLoading()
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            indicator = {
                                TabRowDefaults.PrimaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
                                    color = taskColor,
                                    width = Dp.Unspecified,
                                )
                            },
                        ) {
                            TABS.withIndex().forEach { (index, tab) ->
                                val enabled = index == 0 || (index == 1 && !uiState.noFunctionRecognized)
                                Tab(
                                    selected = selectedTabIndex == index,
                                    enabled = enabled,
                                    onClick = { selectedTabIndex = index },
                                    modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.3f },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            val titleColor = if (selectedTabIndex == index) taskColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            Icon(
                                                tab.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp).alpha(0.7f),
                                                tint = titleColor,
                                            )
                                            BasicText(
                                                text = stringResource(tab.labelResId),
                                                maxLines = 1,
                                                color = { titleColor },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp, stepSize = 1.sp),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        AnimatedContent(
                            selectedTabIndex,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { 40 } + fadeIn() togetherWith slideOutHorizontally { -40 } + fadeOut(androidx.compose.animation.core.tween(50))
                                } else {
                                    slideInHorizontally { -40 } + fadeIn() togetherWith slideOutHorizontally { 40 } + fadeOut(androidx.compose.animation.core.tween(50))
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { index ->
                            if (index == 0) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val cdResponse = stringResource(R.string.cd_model_response_text)
                                    MarkdownText(
                                        text = uiState.modelResponse,
                                        modifier = Modifier.semantics(mergeDescendants = true) {
                                            contentDescription = cdResponse
                                            if (doneGeneratingResponse) liveRegion = LiveRegionMode.Polite
                                        }.padding(16.dp),
                                    )
                                    if (uiState.noFunctionRecognized) {
                                        MessageBodyWarning(ChatMessageWarning(content = stringResource(R.string.warning_no_function_call)))
                                    }
                                }
                            } else if (index == 1) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.functionCallDetails.forEachIndexed { i, details ->
                                        MarkdownText(text = details, modifier = Modifier.padding(16.dp))
                                        if (i != uiState.functionCallDetails.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).graphicsLayer { alpha = if (uiState.processing) 0.5f else 1f },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    PROMPT_TEMPLATES.forEach { item ->
                        Text(
                            stringResource(item.labelResId),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !uiState.processing) { send(item.prompt) }
                                .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(12.dp))
                                .padding(all = 12.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}
