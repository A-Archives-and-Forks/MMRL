package com.dergoogler.mmrl.ui.activity.terminal.action

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dergoogler.mmrl.R
import com.dergoogler.mmrl.app.Event
import com.dergoogler.mmrl.app.Event.Companion.isFinished
import com.dergoogler.mmrl.app.Event.Companion.isLoading
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.dialog.ConfirmDialog
import com.dergoogler.mmrl.ui.providable.LocalUserPreferences
import com.dergoogler.mmrl.viewmodel.ActionViewModel
import com.dergoogler.mmrl.ui.activity.MMRLComponentActivity
import com.dergoogler.mmrl.ui.component.scaffold.Scaffold
import com.dergoogler.mmrl.ui.component.terminal.TerminalView
import kotlinx.coroutines.launch

@Composable
fun ActionScreen(
    viewModel: ActionViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val userPreferences = LocalUserPreferences.current
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    var cancelAction by remember { mutableStateOf(false) }

    val shell = viewModel.shell
    val event = viewModel.event

    val allowCancel = userPreferences.allowCancelAction

    val backHandler = {
        if (allowCancel) {
            when {
                event.isLoading && shell.isAlive -> cancelAction = true
                event.isFinished -> (context as MMRLComponentActivity).finish()
            }
        } else {
            if (event.isFinished) {
                (context as MMRLComponentActivity).finish()
            }
        }
    }

    BackHandler(
        enabled = if (!allowCancel) event.isLoading else true,
        onBack = backHandler
    )
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            viewModel.writeLogsTo(uri)
                .onSuccess {
                    val message = context.getString(R.string.install_logs_saved)
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }.onFailure {
                    val message = context.getString(
                        R.string.install_logs_save_failed,
                        it.message ?: context.getString(R.string.unknown_error)
                    )
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
        }
    }

    if (cancelAction) ConfirmDialog(
        title = R.string.action_screen_cancel_title,
        description = R.string.action_screen_cancel_text,
        onClose = { cancelAction = false },
        onConfirm = {
            scope.launch {
                cancelAction = false
                shell.close()
            }
        }
    )

    Scaffold(
        modifier = Modifier
            .onKeyEvent {
                when (it.key) {
                    Key.VolumeUp, Key.VolumeDown -> event.isLoading

                    else -> false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                exportLog = { launcher.launch(viewModel.logfile) },
                event = event,
                enable = if (!allowCancel) event.isFinished else true,
                scrollBehavior = scrollBehavior,
                onBack = backHandler
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        TerminalView(
            list = viewModel.console,
            state = listState,
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun TopBar(
    exportLog: () -> Unit,
    event: Event,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    enable: Boolean,
) = NavigateUpTopBar(
    title = stringResource(id = R.string.action_activity),
    subtitle = stringResource(
        id = when (event) {
            Event.LOADING -> R.string.action_executing
            Event.FAILED -> R.string.action_failure
            else -> R.string.install_done
        }
    ),
    enable = enable,
    scrollBehavior = scrollBehavior,
    onBack = onBack,
    actions = {
        if (event.isFinished) {
            IconButton(
                onClick = exportLog
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.device_floppy),
                    contentDescription = null
                )
            }
        }
    }
)