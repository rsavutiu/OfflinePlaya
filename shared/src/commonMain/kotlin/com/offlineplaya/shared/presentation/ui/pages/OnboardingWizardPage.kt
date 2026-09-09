package com.offlineplaya.shared.presentation.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlineplaya.shared.presentation.ui.atoms.AppButton
import com.offlineplaya.shared.presentation.ui.atoms.AppCaption
import com.offlineplaya.shared.presentation.ui.atoms.AppHeadline
import com.offlineplaya.shared.presentation.ui.preview.PreviewScreenSizes
import com.offlineplaya.shared.presentation.ui.theme.AppSpacing
import com.offlineplaya.shared.presentation.ui.theme.LocalBrandAccent
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import offlineplaya.shared.generated.resources.Res
import offlineplaya.shared.generated.resources.onboarding_add_music_body
import offlineplaya.shared.generated.resources.onboarding_add_music_found
import offlineplaya.shared.generated.resources.onboarding_add_music_pick_folder
import offlineplaya.shared.generated.resources.onboarding_add_music_title
import offlineplaya.shared.generated.resources.onboarding_add_music_use_device
import offlineplaya.shared.generated.resources.onboarding_back
import offlineplaya.shared.generated.resources.onboarding_done_body
import offlineplaya.shared.generated.resources.onboarding_done_cta
import offlineplaya.shared.generated.resources.onboarding_done_summary
import offlineplaya.shared.generated.resources.onboarding_done_title
import offlineplaya.shared.generated.resources.onboarding_next
import offlineplaya.shared.generated.resources.onboarding_permission_audio_body
import offlineplaya.shared.generated.resources.onboarding_permission_audio_cta
import offlineplaya.shared.generated.resources.onboarding_permission_audio_title
import offlineplaya.shared.generated.resources.onboarding_permission_granted
import offlineplaya.shared.generated.resources.onboarding_permission_notifications_body
import offlineplaya.shared.generated.resources.onboarding_permission_notifications_cta
import offlineplaya.shared.generated.resources.onboarding_permission_notifications_title
import offlineplaya.shared.generated.resources.onboarding_permissions_body
import offlineplaya.shared.generated.resources.onboarding_permissions_title
import offlineplaya.shared.generated.resources.onboarding_skip
import offlineplaya.shared.generated.resources.onboarding_welcome_body
import offlineplaya.shared.generated.resources.onboarding_welcome_cta
import offlineplaya.shared.generated.resources.onboarding_welcome_title
import org.jetbrains.compose.resources.stringResource

/**
 * First-run onboarding shown once (gated by [OnboardingStateHolder] at the
 * Android host), before the main [App]. Walks a new user from install to a
 * playable library: welcome → permissions → add music → done.
 *
 * All Android specifics — permission status and the request/pick launchers —
 * are injected as plain values/lambdas (the same pattern the app uses for
 * `onPickFolder`), so this stays in commonMain and previewable. It carries no
 * navigation of its own; [onFinish] hands control back to the host, which then
 * renders [App].
 *
 * The flow is a step index + [AnimatedContent] rather than a swipe pager on
 * purpose: system permission dialogs interrupt the screen, and a pager's drag
 * gesture fights with dismissing them.
 */
enum class OnboardingStep { WELCOME, PERMISSIONS, ADD_MUSIC, DONE }

@Composable
fun OnboardingWizardPage(
    audioGranted: Boolean,
    notificationGranted: Boolean,
    notificationApplicable: Boolean,
    trackCount: Long,
    folderCount: Int,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onPickFolder: () -> Unit,
    onUseDeviceAudio: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: OnboardingStep = OnboardingStep.WELCOME,
) {
    var step by remember { mutableStateOf(initialStep) }

    Surface(modifier = modifier.fillMaxSize()) {
        // The host (MainActivity) draws edge-to-edge and the wizard is not
        // inside a Scaffold, so inset the content by the safe-drawing area
        // (status bar, nav bar, cutout) while the Surface background still
        // fills the whole window.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // Skip is always available; it finishes onboarding and lands on
            // Home, where the polished empty guide covers a still-empty library.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.End,
            ) {
                if (step != OnboardingStep.DONE) {
                    TextButton(onClick = onFinish) {
                        Text(stringResource(Res.string.onboarding_skip))
                    }
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(250)) { w -> dir * w / 8 } + fadeIn(tween(250)))
                        .togetherWith(
                            slideOutHorizontally(tween(250)) { w -> -dir * w / 8 } + fadeOut(tween(250)),
                        )
                },
                label = "onboarding-step",
                modifier = Modifier.weight(1f),
            ) { current ->
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (current) {
                        OnboardingStep.WELCOME -> WelcomeStep(
                            onNext = { step = OnboardingStep.PERMISSIONS },
                        )

                        OnboardingStep.PERMISSIONS -> PermissionsStep(
                            audioGranted = audioGranted,
                            notificationGranted = notificationGranted,
                            notificationApplicable = notificationApplicable,
                            onRequestAudioPermission = onRequestAudioPermission,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onBack = { step = OnboardingStep.WELCOME },
                            onNext = { step = OnboardingStep.ADD_MUSIC },
                        )

                        OnboardingStep.ADD_MUSIC -> AddMusicStep(
                            trackCount = trackCount,
                            onPickFolder = onPickFolder,
                            onUseDeviceAudio = onUseDeviceAudio,
                            onBack = { step = OnboardingStep.PERMISSIONS },
                            onNext = { step = OnboardingStep.DONE },
                        )

                        OnboardingStep.DONE -> DoneStep(
                            trackCount = trackCount,
                            folderCount = folderCount,
                            onFinish = onFinish,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Spacer(Modifier.height(AppSpacing.xl))
    HeroDisc(Icons.Outlined.LibraryMusic)
    Spacer(Modifier.height(AppSpacing.lg))
    AppHeadline(text = stringResource(Res.string.onboarding_welcome_title))
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(text = stringResource(Res.string.onboarding_welcome_body))
    Spacer(Modifier.height(AppSpacing.xl))
    AppButton(
        text = stringResource(Res.string.onboarding_welcome_cta),
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PermissionsStep(
    audioGranted: Boolean,
    notificationGranted: Boolean,
    notificationApplicable: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Spacer(Modifier.height(AppSpacing.sm))
    AppHeadline(text = stringResource(Res.string.onboarding_permissions_title))
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(text = stringResource(Res.string.onboarding_permissions_body))
    Spacer(Modifier.height(AppSpacing.xl))

    PermissionCard(
        icon = Icons.Outlined.MusicNote,
        title = stringResource(Res.string.onboarding_permission_audio_title),
        body = stringResource(Res.string.onboarding_permission_audio_body),
        cta = stringResource(Res.string.onboarding_permission_audio_cta),
        granted = audioGranted,
        onRequest = onRequestAudioPermission,
    )

    if (notificationApplicable) {
        Spacer(Modifier.height(AppSpacing.md))
        PermissionCard(
            icon = Icons.Outlined.Notifications,
            title = stringResource(Res.string.onboarding_permission_notifications_title),
            body = stringResource(Res.string.onboarding_permission_notifications_body),
            cta = stringResource(Res.string.onboarding_permission_notifications_cta),
            granted = notificationGranted,
            onRequest = onRequestNotificationPermission,
        )
    }

    Spacer(Modifier.height(AppSpacing.xl))
    StepNav(onBack = onBack, onNext = onNext)
}

@Composable
private fun AddMusicStep(
    trackCount: Long,
    onPickFolder: () -> Unit,
    onUseDeviceAudio: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Spacer(Modifier.height(AppSpacing.sm))
    HeroDisc(Icons.Outlined.LibraryMusic)
    Spacer(Modifier.height(AppSpacing.lg))
    AppHeadline(text = stringResource(Res.string.onboarding_add_music_title))
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(text = stringResource(Res.string.onboarding_add_music_body))
    Spacer(Modifier.height(AppSpacing.xl))
    // Primary: index everything MediaStore already knows about (Downloads,
    // Music, …) with no folder pick. Audio access is guaranteed by the gate.
    AppButton(
        text = stringResource(Res.string.onboarding_add_music_use_device),
        onClick = onUseDeviceAudio,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.sm))
    OutlinedButton(
        onClick = onPickFolder,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.onboarding_add_music_pick_folder))
    }
    if (trackCount > 0) {
        Spacer(Modifier.height(AppSpacing.md))
        Text(
            text = stringResource(Res.string.onboarding_add_music_found, trackCount.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(AppSpacing.xl))
    StepNav(onBack = onBack, onNext = onNext)
}

@Composable
private fun DoneStep(
    trackCount: Long,
    folderCount: Int,
    onFinish: () -> Unit,
) {
    Spacer(Modifier.height(AppSpacing.xl))
    HeroDisc(Icons.Filled.CheckCircle)
    Spacer(Modifier.height(AppSpacing.lg))
    AppHeadline(text = stringResource(Res.string.onboarding_done_title))
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(text = stringResource(Res.string.onboarding_done_body))
    Spacer(Modifier.height(AppSpacing.md))
    Text(
        text = stringResource(Res.string.onboarding_done_summary, trackCount.toInt(), folderCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = LocalBrandAccent.current.accent,
    )
    Spacer(Modifier.height(AppSpacing.xl))
    AppButton(
        text = stringResource(Res.string.onboarding_done_cta),
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Brand-accent disc holding an icon — the hero motif shared across steps. */
@Composable
private fun HeroDisc(icon: ImageVector) {
    val brand = LocalBrandAccent.current
    Surface(
        modifier = Modifier.size(96.dp),
        color = brand.accent.copy(alpha = 0.14f),
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = brand.accent,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

/**
 * One permission ask: icon + title + why, and either a request button or a
 * "Granted" confirmation once the user has allowed it.
 */
@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    cta: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(AppSpacing.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(AppSpacing.md))
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LocalBrandAccent.current.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(AppSpacing.xs))
                    Text(
                        text = stringResource(Res.string.onboarding_permission_granted),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalBrandAccent.current.accent,
                    )
                }
            } else {
                AppButton(text = cta, onClick = onRequest)
            }
        }
    }
}

/** Back / Next pair used by the middle steps. */
@Composable
private fun StepNav(onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutlinedButton(onClick = onBack) {
            Text(stringResource(Res.string.onboarding_back))
        }
        AppButton(text = stringResource(Res.string.onboarding_next), onClick = onNext)
    }
}

@PreviewScreenSizes
@Composable
private fun OnboardingWelcomePreview() {
    PreviewTheme(darkTheme = true) {
        OnboardingWizardPage(
            audioGranted = false,
            notificationGranted = false,
            notificationApplicable = true,
            trackCount = 0,
            folderCount = 0,
            onRequestAudioPermission = {},
            onRequestNotificationPermission = {},
            onPickFolder = {},
            onUseDeviceAudio = {},
            onFinish = {},
            initialStep = OnboardingStep.WELCOME,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun OnboardingPermissionsPreview() {
    PreviewTheme(darkTheme = true) {
        OnboardingWizardPage(
            audioGranted = true,
            notificationGranted = false,
            notificationApplicable = true,
            trackCount = 0,
            folderCount = 0,
            onRequestAudioPermission = {},
            onRequestNotificationPermission = {},
            onPickFolder = {},
            onUseDeviceAudio = {},
            onFinish = {},
            initialStep = OnboardingStep.PERMISSIONS,
        )
    }
}

@PreviewScreenSizes
@Composable
private fun OnboardingDonePreview() {
    PreviewTheme(darkTheme = true) {
        OnboardingWizardPage(
            audioGranted = true,
            notificationGranted = true,
            notificationApplicable = true,
            trackCount = 428,
            folderCount = 3,
            onRequestAudioPermission = {},
            onRequestNotificationPermission = {},
            onPickFolder = {},
            onUseDeviceAudio = {},
            onFinish = {},
            initialStep = OnboardingStep.DONE,
        )
    }
}
