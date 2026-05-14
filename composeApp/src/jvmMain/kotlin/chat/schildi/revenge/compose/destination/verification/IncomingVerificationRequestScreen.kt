package chat.schildi.revenge.compose.destination.verification

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Destination
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.actionProvider
import chat.schildi.revenge.actions.plainTextCopyAction
import chat.schildi.revenge.actions.plainTextCopyActionWithMxcUrl
import chat.schildi.revenge.compose.components.AvatarImage
import chat.schildi.revenge.compose.components.TopNavigation
import chat.schildi.revenge.compose.components.TopNavigationCloseOrNavigateToInboxIcon
import chat.schildi.revenge.compose.components.TopNavigationTitle
import chat.schildi.revenge.compose.focus.FocusContainer
import chat.schildi.revenge.compose.focus.keyFocusable
import chat.schildi.revenge.model.account.ScIncomingVerificationRequest
import chat.schildi.revenge.model.verification.IncomingVerificationRequestViewModel
import chat.schildi.revenge.model.verification.toEmojiResource
import chat.schildi.revenge.publishTitle
import chat.schildi.revenge.viewModelKey
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.verification.SessionVerificationData
import io.element.android.libraries.matrix.api.verification.VerificationEmoji
import io.element.android.libraries.matrix.api.verification.VerificationFlowState
import io.element.android.libraries.matrix.api.verification.VerificationRequest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_accept
import shire.composeapp.generated.resources.action_cancel
import shire.composeapp.generated.resources.action_decline
import shire.composeapp.generated.resources.action_done
import shire.composeapp.generated.resources.action_start_emoji_verification
import shire.composeapp.generated.resources.action_verification_match
import shire.composeapp.generated.resources.action_verification_mismatch
import shire.composeapp.generated.resources.verification_cancelled
import shire.composeapp.generated.resources.verification_compare_decimals_match
import shire.composeapp.generated.resources.verification_compare_emoji_match
import shire.composeapp.generated.resources.verification_device_details_device_id
import shire.composeapp.generated.resources.verification_device_details_device_name
import shire.composeapp.generated.resources.verification_device_details_flow_id
import shire.composeapp.generated.resources.verification_device_details_user_id
import shire.composeapp.generated.resources.verification_device_first_seen
import shire.composeapp.generated.resources.verification_device_header_type_other_user
import shire.composeapp.generated.resources.verification_device_header_type_self
import shire.composeapp.generated.resources.verification_failed
import shire.composeapp.generated.resources.verification_incoming_request_title
import shire.composeapp.generated.resources.verification_successful

@Composable
fun IncomingVerificationRequestScreen(
    destination: Destination.IncomingVerificationRequest,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val viewModel: IncomingVerificationRequestViewModel = viewModel(
        key = viewModelKey(destination),
        factory = viewModelFactory { initializer { IncomingVerificationRequestViewModel(destination.request) } },
    )
    publishTitle(viewModel)

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.resetOnDispose()
        }
    }

    val verificationFlowState = viewModel.verificationFlowState.collectAsState().value
    FocusContainer(
        modifier = modifier,
        role = FocusRole.DESTINATION_ROOT_CONTAINER,
    ) {
        Column {
            TopNavigation {
                TopNavigationTitle(stringResource(Res.string.verification_incoming_request_title))
                TopNavigationCloseOrNavigateToInboxIcon()
            }
            Box(contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(Dimens.windowPadding),
                    verticalArrangement = Dimens.verticalArrangementBig,
                ) {
                    val request = destination.request
                    when (verificationFlowState) {
                        VerificationFlowState.Initial -> VerificationInitialContent(request, viewModel)
                        VerificationFlowState.DidAcceptVerificationRequest -> VerificationAcceptedContent(request, viewModel)
                        VerificationFlowState.DidStartSasVerification -> VerificationSasStartedContent(request, viewModel)
                        is VerificationFlowState.DidReceiveVerificationData -> VerificationDataContent(verificationFlowState, request, viewModel)
                        VerificationFlowState.DidFinish -> VerificationFinishedContent(request)
                        VerificationFlowState.DidCancel -> VerificationCancelledContent(request)
                        VerificationFlowState.DidFail -> VerificationFailedContent(request)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.VerificationMetadataInfo(
    request: ScIncomingVerificationRequest,
    modifier: Modifier = Modifier,
) {
    Column(modifier.align(Alignment.CenterHorizontally), verticalArrangement = Dimens.verticalArrangement) {
        Text(
            when (request.request) {
                is VerificationRequest.Incoming.OtherSession -> stringResource(Res.string.verification_device_header_type_self)
                is VerificationRequest.Incoming.User -> stringResource(Res.string.verification_device_header_type_other_user)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        val details = request.request.details
        if (details.senderProfile.userId != request.sessionId) {
            details.senderProfile.avatarUrl?.let { avatarUrl ->
                AvatarImage(
                    source = MediaSource(avatarUrl),
                    size = 128.dp,
                    displayName = details.senderProfile.displayName ?: details.senderProfile.userId.value,
                    modifier = Modifier.keyFocusable(
                        role = FocusRole.LIST_ITEM,
                        actionProvider = actionProvider(
                            copyActions = plainTextCopyActionWithMxcUrl(avatarUrl),
                        )
                    ),
                )
            }
            VerificationMetadataInfoRow(
                stringResource(Res.string.verification_device_details_user_id),
                details.senderProfile.userId.value,
            )
        }
        VerificationMetadataInfoRow(
            stringResource(Res.string.verification_device_details_user_id),
            details.senderProfile.userId.value,
        )
        VerificationMetadataInfoRow(
            stringResource(Res.string.verification_device_details_device_name),
            details.deviceDisplayName,
        )
        VerificationMetadataInfoRow(
            stringResource(Res.string.verification_device_details_device_id),
            details.deviceId.value,
        )
        VerificationMetadataInfoRow(
            stringResource(Res.string.verification_device_details_flow_id),
            details.flowId.value,
        )
        VerificationMetadataInfoRow(
            stringResource(Res.string.verification_device_first_seen),
            DateTimeFormat.formatTimeOrDateTime(details.firstSeenTimestamp),
        )
    }
}

@Composable
private fun VerificationMetadataInfoRow(
    title: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    value ?: return
    Row(
        modifier.keyFocusable(
            role = FocusRole.LIST_ITEM,
            actionProvider = actionProvider(
                copyActions = plainTextCopyAction { value },
            )
        ),
        horizontalArrangement = Dimens.horizontalArrangement,
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun VerificationStateText(
    text: String,
    critical: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().keyFocusable(
            role = FocusRole.LIST_ITEM,
            actionProvider = actionProvider(
                copyActions = plainTextCopyAction { text },
            ),
        ),
    )
}

@Composable
private fun VerificationButton(
    text: String,
    modifier: Modifier = Modifier,
    critical: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = if (critical)
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        else
            ButtonDefaults.buttonColors(),
        modifier = modifier.fillMaxWidth()
            .keyFocusable(
                role = FocusRole.LIST_ITEM,
                actionProvider = actionProvider(
                    primaryAction = InteractionAction.Invoke {
                        onClick()
                        true
                    },
                ),
                addClickListener = false,
            ),
    ) {
        Text(text)
    }
}

@Composable
private fun VerificationLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun VerificationButtonSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier,
        verticalArrangement = Dimens.verticalArrangement,
        content = content,
    )
}

@Composable
private fun ColumnScope.VerificationInitialContent(
    request: ScIncomingVerificationRequest,
    viewModel: IncomingVerificationRequestViewModel,
) {
    VerificationMetadataInfo(request)
    VerificationButtonSection {
        VerificationButton(stringResource(Res.string.action_accept)) {
            viewModel.acceptVerificationRequest()
        }
        VerificationButton(stringResource(Res.string.action_decline)) {
            viewModel.cancelVerification()
        }
    }
}

@Composable
private fun ColumnScope.VerificationAcceptedContent(
    request: ScIncomingVerificationRequest,
    viewModel: IncomingVerificationRequestViewModel,
) {
    VerificationMetadataInfo(request)
    VerificationButtonSection {
        VerificationButton(stringResource(Res.string.action_start_emoji_verification)) {
            viewModel.startSasVerification()
        }
        VerificationButton(stringResource(Res.string.action_cancel)) {
            viewModel.cancelVerification()
        }
    }
}

@Composable
private fun ColumnScope.VerificationSasStartedContent(
    request: ScIncomingVerificationRequest,
    viewModel: IncomingVerificationRequestViewModel,
) {
    VerificationMetadataInfo(request)
    VerificationLoadingIndicator()
    VerificationButtonSection {
        VerificationButton(stringResource(Res.string.action_cancel)) {
            viewModel.cancelVerification()
        }
    }
}

@Composable
private fun ColumnScope.VerificationDataContent(
    state: VerificationFlowState.DidReceiveVerificationData,
    request: ScIncomingVerificationRequest,
    viewModel: IncomingVerificationRequestViewModel,
) {
    VerificationMetadataInfo(request)
    val stateText = when (state.data) {
        is SessionVerificationData.Emojis -> stringResource(Res.string.verification_compare_emoji_match)
        is SessionVerificationData.Decimals -> stringResource(Res.string.verification_compare_decimals_match)
    }
    VerificationStateText(stateText)
    when (val data = state.data) {
        is SessionVerificationData.Emojis -> VerificationDataEmojiContent(data, Modifier.fillMaxWidth())
        is SessionVerificationData.Decimals -> VerificationStateText(data.decimals.joinToString())
    }
    val hasApproved = viewModel.pendingApproveAck.collectAsState().value
    if (hasApproved) {
        VerificationLoadingIndicator()
    }
    VerificationButtonSection {
        if (!hasApproved) {
            VerificationButton(stringResource(Res.string.action_verification_match)) {
                viewModel.approveVerification()
            }
            VerificationButton(stringResource(Res.string.action_verification_mismatch), critical = true) {
                viewModel.declineVerification()
            }
        } else {
            VerificationButton(stringResource(Res.string.action_cancel)) {
                viewModel.cancelVerification()
            }
        }
    }
}

@Composable
private fun VerificationDataEmojiContent(
    data: SessionVerificationData.Emojis,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.horizontalItemPaddingBig, Alignment.CenterHorizontally),
        verticalArrangement = Dimens.verticalArrangement,
        itemVerticalAlignment = Alignment.Top,
    ) {
        data.emojis.forEach { emoji ->
            VerificationEmojiContent(emoji)
        }
    }
}

@Composable
private fun VerificationEmojiContent(
    emoji: VerificationEmoji,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Dimens.verticalArrangementSmall,
    ) {
        val resource = emoji.number.toEmojiResource()
        Image(
            painterResource(resource.drawableRes),
            stringResource(resource.nameRes),
            modifier = Modifier.size(72.dp),
        )
        Text(stringResource(resource.nameRes))
    }
}

@Composable
private fun ColumnScope.VerificationFinishedContent(
    request: ScIncomingVerificationRequest,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationState = LocalDestinationState.current
    VerificationStateText(stringResource(Res.string.verification_successful))
    VerificationMetadataInfo(request)
    VerificationButtonSection {
        VerificationButton(stringResource(Res.string.action_done)) {
            destinationState?.closeScreen(keyHandler)
        }
    }
}

@Composable
private fun ColumnScope.VerificationCancelledContent(
    request: ScIncomingVerificationRequest,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationState = LocalDestinationState.current
    VerificationStateText(stringResource(Res.string.verification_cancelled), critical = true)
    VerificationMetadataInfo(request)
    VerificationButtonSection {
        VerificationButton(stringResource(Res.string.action_done)) {
            destinationState?.closeScreen(keyHandler)
        }
    }
}

@Composable
private fun ColumnScope.VerificationFailedContent(
    request: ScIncomingVerificationRequest,
) {
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationState = LocalDestinationState.current
    VerificationStateText(stringResource(Res.string.verification_failed), critical = true)
    VerificationMetadataInfo(request)
    VerificationButtonSection {
        VerificationButton(stringResource(Res.string.action_done)) {
            destinationState?.closeScreen(keyHandler)
        }
    }
}
