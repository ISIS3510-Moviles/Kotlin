package com.example.campusbites.di

import com.example.campusbites.data.local.LocalAlertDataSource
import com.example.campusbites.data.local.RealmAlertDataSource
import com.example.campusbites.data.repository.AuthRepositoryImpl
import com.example.campusbites.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLocalAlertDataSource(
        realmAlertDataSource: RealmAlertDataSource
    ): LocalAlertDataSource

}