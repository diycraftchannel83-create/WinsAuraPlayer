package com.example.model

/**
 * Data class representing all editable ID3 metadata fields of an MP3 file.
 */
data class Mp3Metadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val composer: String = "",
    val comment: String = "",
    val subtitle: String = "",
    val coverUrl: String = ""
)
