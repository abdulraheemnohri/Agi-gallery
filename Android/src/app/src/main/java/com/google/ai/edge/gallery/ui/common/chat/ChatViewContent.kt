package com.google.ai.edge.gallery.ui.common.chat

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

@Composable
fun ChatViewContent(
    task: Task,
    selectedModel: Model,
    viewModel: ChatViewModel,
    modelManagerViewModel: ModelManagerViewModel,
    skillCount: Int = 0,
    mcpCount: Int = 0,
    onSendMessage: (Model, List<ChatMessage>) -> Unit,
    onRunAgainClicked: (Model, ChatMessage) -> Unit,
    onBenchmarkClicked: (Model, ChatMessage, Int, Int) -> Unit,
    onStopButtonClicked: () -> Unit,
    onSkillClicked: () -> Unit,
    onMcpClicked: () -> Unit,
    onImageSelected: (List<Bitmap>, Int) -> Unit = { _, _ -> },
    showImagePicker: Boolean = false,
    showAudioPicker: Boolean = false,
    modifier: Modifier = Modifier
) {
    ChatPanel(
        modelManagerViewModel = modelManagerViewModel,
        task = task,
        selectedModel = selectedModel,
        viewModel = viewModel,
        innerPadding = PaddingValues(),
        skillCount = skillCount,
        mcpCount = mcpCount,
        navigateUp = {},
        onSendMessage = onSendMessage,
        onRunAgainClicked = onRunAgainClicked,
        onBenchmarkClicked = onBenchmarkClicked,
        onStopButtonClicked = onStopButtonClicked,
        onSkillClicked = onSkillClicked,
        onMcpClicked = onMcpClicked,
        onImageSelected = onImageSelected,
        showImagePicker = showImagePicker,
        showAudioPicker = showAudioPicker,
        modifier = modifier
    )
}
