package com.offlineplaya.android.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * Wraps [Intent.ACTION_OPEN_DOCUMENT_TREE]. Pass `requestWrite = true` to
 * also request `FLAG_GRANT_WRITE_URI_PERMISSION` — needed when we plan to
 * write album art back into the user's files.
 *
 * Returns the picked tree URI or `null` if the user cancelled. The
 * persistable-permission grant must still be taken via
 * `contentResolver.takePersistableUriPermission(uri, …)` after a non-null
 * result — the contract itself can't do that.
 */
class OpenDocumentTreeContract(
    private val requestWrite: Boolean = false,
) : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            if (requestWrite) {
                flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            addFlags(flags)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
