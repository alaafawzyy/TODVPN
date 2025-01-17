package io.norselabs.vpn.based.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.norselabs.vpn.based.app_config.AppConfig
import io.norselabs.vpn.common_network.AppRepository
import io.norselabs.vpn.common_network.DnsBasedClient
import io.norselabs.vpn.common_network.HeadersInterceptor
import io.norselabs.vpn.common_network.api.Api
import io.norselabs.vpn.common_network.api.ConnectApi
import io.norselabs.vpn.core_vpn.storage.CoreStorage
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

  @Provides
  @Singleton
  fun provideHeadersInterceptor(
    storage: CoreStorage,
  ): HeadersInterceptor = HeadersInterceptor(
    userTokenProvider = { storage.getToken() },
  )

  @Provides
  @Singleton
  @Named("ConsoleLogger")
  fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
      setLevel(HttpLoggingInterceptor.Level.BODY)
    }
  }

  @Provides
  @Singleton
  @Named("FileLogger")
  fun provideHttpFileLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor { message ->
      Timber.tag("NetworkLog").d(message)
    }.apply {
      setLevel(HttpLoggingInterceptor.Level.BASIC)
    }
  }

  @Provides
  @Singleton
  fun provideOkHttp(
    headersInterceptor: HeadersInterceptor,
    @Named("ConsoleLogger") loggingInterceptor: HttpLoggingInterceptor,
    @Named("FileLogger") fileLoggingInterceptor: HttpLoggingInterceptor,
  ): OkHttpClient {
    return OkHttpClient.Builder()
      .addInterceptor(headersInterceptor)
      .addInterceptor(loggingInterceptor)
      .addInterceptor(fileLoggingInterceptor)
      .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(
    client: OkHttpClient,
    config: AppConfig,
  ): Retrofit = Retrofit.Builder()
    .baseUrl(config.getBaseUrl())
    .addConverterFactory(GsonConverterFactory.create())
    .client(client)
    .build()

  @Provides
  @Singleton
  fun provideApi(retrofit: Retrofit): Api = retrofit.create(Api::class.java)

  @Provides
  @Singleton
  fun provideConnectApi(
    config: AppConfig,
    headersInterceptor: HeadersInterceptor,
    @Named("ConsoleLogger") loggingInterceptor: HttpLoggingInterceptor,
    @Named("FileLogger") fileLoggingInterceptor: HttpLoggingInterceptor,
  ): ConnectApi {

    val client = OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .addInterceptor(headersInterceptor)
      .addInterceptor(loggingInterceptor)
      .addInterceptor(fileLoggingInterceptor)
      .build()

    val retrofit = Retrofit.Builder()
      .baseUrl(config.getBaseUrl())
      .addConverterFactory(GsonConverterFactory.create())
      .client(client)
      .build()

    return retrofit.create(ConnectApi::class.java)
  }

  @Provides
  @Singleton
  fun provideDnsBasedClient(api: Api): DnsBasedClient {
    return DnsBasedClient(api)
  }

  @Provides
  @Singleton
  fun provideRepository(
    api: Api,
    connectApi: ConnectApi,
    client: OkHttpClient,
    dnsBasedClient: DnsBasedClient,
    config: AppConfig,
  ): AppRepository = AppRepository(
    api = api,
    connectApi = connectApi,
    client = client,
    dnsClient = dnsBasedClient,
    appToken = config.getAppToken(),
  )
}
