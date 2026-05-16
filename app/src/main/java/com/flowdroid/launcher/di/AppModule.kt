package com.flowdroid.launcher.di

import android.content.Context
import androidx.room.Room
import com.flowdroid.launcher.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FlowDroidDatabase =
        Room.databaseBuilder(ctx, FlowDroidDatabase::class.java, "flowdroid.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAppDao(db: FlowDroidDatabase): AppDao = db.appDao()
    @Provides fun provideFolderDao(db: FlowDroidDatabase): FolderDao = db.folderDao()
    @Provides fun provideContainerDao(db: FlowDroidDatabase): ContainerDao = db.containerDao()
}
