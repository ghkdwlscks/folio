package com.portfolio.manager.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.portfolio.manager.data.repository.FirebaseSyncDataSource
import com.portfolio.manager.data.repository.SyncRepositoryImpl
import com.portfolio.manager.domain.repository.SyncDataSource
import com.portfolio.manager.domain.repository.SyncRepository

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideSyncDataSource(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): SyncDataSource = FirebaseSyncDataSource(auth, firestore)

    @Provides
    @Singleton
    fun provideSyncRepository(dataSource: SyncDataSource): SyncRepository =
        SyncRepositoryImpl(dataSource)
}
