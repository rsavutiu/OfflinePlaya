package com.offlineplaya.android.picker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Wrapper around [Intent.ACTION_OPEN_DOCUMENT_TREE] that opens the system
 * picker **freshly at the primary storage volume's root** instead of letting
 * DocumentsUI restore whatever location it last browsed.
 *
 * Why: on at least one device (moto g54 / Android 15) the picker would
 * restore into the currently-playing folder, then show empty listings every
 * level up to the storage root — the user couldn't see sibling folders or
 * navigate. Seeding a fresh entry point at the primary volume root via
 * [StorageManager.getPrimaryStorageVolume] +
 * `StorageVolume.createOpenDocumentTreeIntent()` (API 29+, the canonical
 * Google-recommended approach) overrides that stuck restored stack and
 * routes through the AOSP external-storage provider, where top-level folders
 * enumerate reliably. It also pre-fills `EXTRA_INITIAL_URI` for us — no
 * hand-built URIs.
 *
 * Below API 29 we fall back to the vanilla [ActivityResultContracts.OpenDocumentTree]
 * intent (no initial location). The earlier "EXTRA_INITIAL_URI half-breaks the
 * picker" note was a misdiagnosis — it conflated the initial-location *hint*
 * with the `FLAG_GRANT_PERSISTABLE_URI_PERMISSION` flag (which genuinely did
 * trigger the "to protect your privacy" block). We add no grant flags on the
 * launch intent; the persistable permission is taken in `MainActivity` after
 * the user picks, matching Google's samples.
 *
 * NOTE: `createOpenDocumentTreeIntent()` lands the user *at* the volume root
 * to navigate down from, but the root itself is not selectable — they drill
 * into a music sub-folder and pick that, which is exactly the intended flow.
 * The OS still blocks granting the standard media dirs (`Music/`, `Download/`,
 * etc.) and the raw storage root without MANAGE_EXTERNAL_STORAGE (disallowed
 * for music players on Play), so a sub-folder pick remains necessary there.
 */
class OpenDocumentTreeContract(
    private val requestWrite: Boolean = false,
) : ActivityResultContract<Unit, Uri?>() {

    private val standard = ActivityResultContracts.OpenDocumentTree()

    override fun createIntent(context: Context, input: Unit): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val storageManager = context.getSystemService(StorageManager::class.java)
            val seeded = storageManager?.primaryStorageVolume?.createOpenDocumentTreeIntent()
            if (seeded != null) return seeded
        }
        return standard.createIntent(context, null)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        standard.parseResult(resultCode, intent)
}
