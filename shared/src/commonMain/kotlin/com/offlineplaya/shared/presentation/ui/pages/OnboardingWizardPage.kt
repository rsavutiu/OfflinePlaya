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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LibraryMusic
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
import offlineplaya.shared.generated.resources.onboarding_permission_granted
import offlineplaya.shared.generated.resources.onboarding_permission_notifications_body
import offlineplaya.shared.generated.resources.onboarding_permission_notifications_cta
import offlineplaya.shared.generated.resources.onboarding_permission_notifications_title
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
    notificationGranted: Boolean,
    notificationApplicable: Boolean,
    trackCount: Long,
    folderCount: Int,
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
                // Cap the step content to a readable column and centre it.
                // Without this, a landscape tablet stretches the hero disc,
                // text and full-width buttons across the entire window — sparse
                // and broken-looking. Below 480dp (every phone) the cap never
                // bites, so the portrait layout is unchanged. The scrolling
                // content is vertically centred when it fits, so it no longer
                // strands at the top of a tall/wide canvas.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Audio access is a hard prerequisite handled by the gate
                    // before this wizard ever shows, so the only permission left
                    // to ask about is notifications — and only on API 33+. Below
                    // that, skip the step entirely (welcome → add music).
                    when (current) {
                        OnboardingStep.WELCOME -> WelcomeStep(
                            onNext = {
                                step = if (notificationApplicable) {
                                    OnboardingStep.PERMISSIONS
                                } else {
                                    OnboardingStep.ADD_MUSIC
                                }
                            },
                        )

                        OnboardingStep.PERMISSIONS -> NotificationsStep(
                            notificationGranted = notificationGranted,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onBack = { step = OnboardingStep.WELCOME },
                            onNext = { step = OnboardingStep.ADD_MUSIC },
                        )

                        OnboardingStep.ADD_MUSIC -> AddMusicStep(
                            trackCount = trackCount,
                            onPickFolder = onPickFolder,
                            onUseDeviceAudio = onUseDeviceAudio,
                            onBack = {
                                step = if (notificationApplicable) {
                                    OnboardingStep.PERMISSIONS
                                } else {
                                    OnboardingStep.WELCOME
                                }
                            },
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
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    HeroDisc(Icons.Outlined.LibraryMusic)
    Spacer(Modifier.height(AppSpacing.lg))
    AppHeadline(
        text = stringResource(Res.string.onboarding_welcome_title),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(
        text = stringResource(Res.string.onboarding_welcome_body),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.xl))
    AppButton(
        text = stringResource(Res.string.onboarding_welcome_cta),
        onClick = onNext,
        modifier = Modifier.onboardingButton(),
    )
}

@Composable
private fun NotificationsStep(
    notificationGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    HeroDisc(Icons.Outlined.Notifications)
    Spacer(Modifier.height(AppSpacing.lg))
    AppHeadline(
        text = stringResource(Res.string.onboarding_permission_notifications_title),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(
        text = stringResource(Res.string.onboarding_permission_notifications_body),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.xl))
    if (notificationGranted) {
        Row(
            modifier = Modifier.height(OnboardingButtonHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
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
        AppButton(
            text = stringResource(Res.string.onboarding_permission_notifications_cta),
            onClick = onRequestNotificationPermission,
            modifier = Modifier.onboardingButton(),
        )
    }
    Spacer(Modifier.height(AppSpacing.lg))
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
    HeroDisc(Icons.Outlined.LibraryMusic)
    Spacer(Modifier.height(AppSpacing.lg))
    AppHeadline(
        text = stringResource(Res.string.onboarding_add_music_title),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(
        text = stringResource(Res.string.onboarding_add_music_body),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.xl))
    // Primary: index everything MediaStore already knows about (Downloads,
    // Music, …) with no folder pick. Audio access is guaranteed by the gate.
    AppButton(
        text = stringResource(Res.string.onboarding_add_music_use_device),
        onClick = onUseDeviceAudio,
        modifier = Modifier.onboardingButton(),
    )
    Spacer(Modifier.height(AppSpacing.sm))
    OutlinedButton(
        onClick = onPickFolder,
        modifier = Modifier.onboardingButton(),
    ) {
        Text(stringResource(Res.string.onboarding_add_music_pick_folder))
    }
    if (trackCount > 0) {
        Spacer(Modifier.height(AppSpacing.md))
        Text(
            text = stringResource(Res.string.onboarding_add_music_found, trackCount.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
    Spacer(Modifier.height(AppSpacing.lg))
    StepNav(onBack = onBack, onNext = onNext)
}

@Composable
private fun DoneStep(
    trackCount: Long,
    folderCount: Int,
    onFinish: () -> Unit,
) {
    HeroDisc(Icons.Filled.CheckCircle)
    Spacer(Modifier.height(AppSpacing.lg))
    AppHeadline(
        text = stringResource(Res.string.onboarding_done_title),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.sm))
    AppCaption(
        text = stringResource(Res.string.onboarding_done_body),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.md))
    Text(
        text = stringResource(Res.string.onboarding_done_summary, trackCount.toInt(), folderCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = LocalBrandAccent.current.accent,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    Spacer(Modifier.height(AppSpacing.xl))
    AppButton(
        text = stringResource(Res.string.onboarding_done_cta),
        onClick = onFinish,
        modifier = Modifier.onboardingButton(),
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
 * Uniform action-button height shared by every onboarding CTA, primary or
 * outlined, so the stacked buttons read as one consistent control column
 * instead of a ragged pile of differently-sized chips.
 */
private val OnboardingButtonHeight = 52.dp

/** Modifier every onboarding button uses: full column width, uniform height. */
private fun Modifier.onboardingButton(): Modifier = fillMaxWidth().height(OnboardingButtonHeight)

/**
 * Back / Next pair used by the middle steps. Two equal-width buttons filling
 * the column (not edge-shoved wrap-content chips), so they line up with the
 * full-width CTAs above them.
 */
@Composable
private fun StepNav(onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f).height(OnboardingButtonHeight),
        ) {
            Text(stringResource(Res.string.onboarding_back))
        }
        AppButton(
            text = stringResource(Res.string.onboarding_next),
            onClick = onNext,
            modifier = Modifier.weight(1f).height(OnboardingButtonHeight),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun OnboardingWelcomePreview() {
    PreviewTheme(darkTheme = true) {
        OnboardingWizardPage(
            notificationGranted = false,
            notificationApplicable = true,
            trackCount = 0,
            folderCount = 0,
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
            notificationGranted = false,
            notificationApplicable = true,
            trackCount = 0,
            folderCount = 0,
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
            notificationGranted = true,
            notificationApplicable = true,
            trackCount = 428,
            folderCount = 3,
            onRequestNotificationPermission = {},
            onPickFolder = {},
            onUseDeviceAudio = {},
            onFinish = {},
            initialStep = OnboardingStep.DONE,
        )
    }
}
