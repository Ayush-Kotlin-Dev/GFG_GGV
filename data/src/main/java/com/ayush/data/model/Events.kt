package com.ayush.data.model

import kotlinx.serialization.Serializable
import java.io.Serial


@Serializable
data class Event(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val time: String = "",
    val registrationDeadline: String = "",
    val formLink: String = "",
    val imageRes: String = "",
    val description: String = ""
) {
    constructor() : this("", "", "", "", "", "", "", "")
}