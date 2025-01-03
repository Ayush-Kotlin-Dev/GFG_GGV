package com.ayush.geeksforgeeks.utils

import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    val tag_name: String,
    val assets: List<GithubAsset>,
    // Add other fields you might need
    val name: String? = null,
    val body: String? = null
)

@Serializable
data class GithubAsset(
    val browser_download_url: String,
    val name: String,
    // Add other fields you might need
    val size: Long? = null,
    val download_count: Long? = null
)
