package com.ayush.data.di

import com.ayush.data.datastore.UserPreferences
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.MentorRepository
import com.ayush.data.repository.MentorshipRepository
import com.ayush.data.repository.QueryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

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

    @Provides
    @Singleton
    fun provideQueryRepository(firestore: FirebaseFirestore): QueryRepository {
        return QueryRepository(firestore)
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideMentorshipRepository(
        firestore: FirebaseFirestore,
        userPreferences: UserPreferences,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MentorshipRepository {
        return MentorshipRepository(firestore, userPreferences, ioDispatcher)
    }
    
    @Provides
    @Singleton
    fun provideMentorRepository(
        firestore: FirebaseFirestore,
        userPreferences: UserPreferences,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MentorRepository {
        return MentorRepository(firestore, userPreferences, ioDispatcher)
    }
}