package com.ayush.data.repository

import com.ayush.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class HomeRepository @Inject constructor(
    firestore: FirebaseFirestore
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
    suspend fun createEvent(event: Event): Boolean {
        return try {
            eventsCollection.add(event).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}