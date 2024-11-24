package com.ayush.data.repository

import android.net.Uri
import com.ayush.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class HomeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val eventsCollection = firestore.collection("events")

    suspend fun fetchEvents(): List<Event> {
        return try {
            val querySnapshot = eventsCollection.get().await()
            val events = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Event::class.java)?.copy(id = document.id)
            }
            events
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun createEventWithImage(event: Event, imageUri: Uri): Boolean {
        return try {
            val imageUrl = uploadImage(imageUri)
            val eventWithImage = event.copy(imageRes = imageUrl ?: "")
            eventsCollection.add(eventWithImage).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun uploadImage(uri: Uri): String? {
        val imageRef = storage.reference.child("event_images/${System.currentTimeMillis()}.jpg")
        return try {
            val uploadTask = imageRef.putFile(uri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}