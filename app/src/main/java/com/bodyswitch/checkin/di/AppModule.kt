package com.bodyswitch.checkin.di

import com.bodyswitch.checkin.BuildConfig
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.network.AdminTokenInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 체크인앱의 운영 서버다. 호스트명에 "dev"가 들어가지만 개발 서버가 아니다.
    // 릴리스 빌드도 이 주소를 쓴다 (buildType 분기 없음).
    private const val BASE_URL = "https://api-dev.bodyswitch.co.kr/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun newOkHttpBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        // 안면 이미지(base64 대용량) 업로드 대비: 기본 10s로는 부족할 수 있음
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // 릴리스에서는 로깅 비활성화 (대용량 base64 바디 문자열화/로그 유출/GC 부담 방지)
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )

    @Provides
    @Singleton
    fun provideOkHttpClient(adminTokenInterceptor: AdminTokenInterceptor): OkHttpClient =
        newOkHttpBuilder()
            .addInterceptor(adminTokenInterceptor)
            .build()

    // 토큰 자동 갱신용 재로그인 전용 클라이언트. AdminTokenInterceptor를 달지 않아
    // 인터셉터 안에서의 재로그인 호출이 자기 자신을 재진입하지 않게 한다.
    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshOkHttpClient(): OkHttpClient = newOkHttpBuilder().build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshRetrofit(
        @Named("refresh") okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideKioskApi(retrofit: Retrofit): KioskApi =
        retrofit.create(KioskApi::class.java)

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshKioskApi(@Named("refresh") retrofit: Retrofit): KioskApi =
        retrofit.create(KioskApi::class.java)
}
