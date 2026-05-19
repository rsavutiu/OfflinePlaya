package com.offlineplaya.shared.domain.scanner

/**
 * The output of [FolderScanner.scan]: every folder reachable from the tree root
 * (parents before children) and every audio file discovered.
 */
data class ScanResult(
    val folders: List<AudioFolder>,
    val files: List<RawAudioFile>,
)
