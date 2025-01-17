package io.norselabs.vpn.based.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.norselabs.vpn.based.app_config.AppConfig
import io.norselabs.vpn.core_vpn.storage.CoreStorage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StorageModule {

  @Provides
  @Singleton
  fun provideSharedPreferences(
    @ApplicationContext context: Context,
    config: AppConfig,
  ): SharedPreferences = context.getSharedPreferences(config.getPackage(), Context.MODE_PRIVATE)

  @Provides
  @Singleton
  fun provideGson() = Gson()

  @Provides
  @Singleton
  fun provideCoreStorage(
    prefs: SharedPreferences,
  ): CoreStorage {
    return CoreStorage(prefs)
  }
}
