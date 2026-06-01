package com.offlineplaya.android.picker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Thin wrapper around [Intent.ACTION_OPEN_DOCUMENT_TREE].
 *
 * Deliberately minimal: no `EXTRA_INITIAL_URI` (DocumentsUI half-breaks the
 * picker when seeded with a URI the app hasn't been granted access to yet),
 * no `FLAG_GRANT_PERSISTABLE_URI_PERMISSION` on the launch intent (which
 * makes the picker stricter about which sub-trees it allows). The
 * persistable permission is taken in `MainActivity` after the user picks,
 * which is what Google's own samples do.
 *
 * The OS-level block on standard media dirs (`Music/`, `Download/`,
 * `Pictures/`, etc.) and the storage root cannot be bypassed without
 * MANAGE_EXTERNAL_STORAGE — which Google Play disallows for music players.
 * Users have to drill into a sub-folder of those to pick. The home-page
 * hint copy explains that.
 */
class OpenDocumentTreeContract(
    private val requestWrite: Boolean = false,
) : ActivityResultContract<Unit, Uri?>() {

    private val standard = ActivityResultContracts.OpenDocumentTree()

    override fun createIntent(context: Context, input: Unit): Intent =
        standard.createIntent(context, null)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        standard.parseResult(resultCode, intent)
}
