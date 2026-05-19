package com.offlineplaya.android.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * Wraps [Intent.ACTION_OPEN_DOCUMENT_TREE] so callers can use
 * `rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri -> … }`.
 *
 * Returns the picked tree URI or `null` if the user cancelled. Note: the
 * persistable-permission grant must still be taken via
 * `contentResolver.takePersistableUriPermission(uri, …)` after a non-null
 * result — the contract itself can't do that.
 */
class OpenDocumentTreeContract : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
