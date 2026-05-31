package com.offlineplaya.android.picker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Robust wrapper for [Intent.ACTION_OPEN_DOCUMENT_TREE].
 * 
 * NOTE: We do NOT add extra flags like FLAG_GRANT_PERSISTABLE_URI_PERMISSION here
 * because doing so triggers the "To protect your privacy" error in the system 
 * picker for many subfolders. We take the persistable permission AFTER the 
 * user has picked the folder in MainActivity.
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
