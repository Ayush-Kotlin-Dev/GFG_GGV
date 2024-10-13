package com.ayush.data.di

import com.ayush.data.AuthRepository
import com.ayush.data.datastore.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        userPreferences: UserPreferences
    ): AuthRepository {
        return AuthRepository(
            firebaseAuth,
            firestore,
            userPreferences
        )
    }
}