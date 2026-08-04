package com.bodyswitch.checkin.di

import com.bodyswitch.checkin.BuildConfig
import com.bodyswitch.checkin.data.api.KioskApi
import com.bodyswitch.checkin.data.network.AdminTokenInterceptor
import com.bodyswitch.checkin.data.network.RetryInterceptor
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

    // BuildConfig.BASE_URL 은 app/build.gradle.kts 에서 주입한다.
    //  - release : https://api-dev.bodyswitch.co.kr/  (호스트명에 "dev"가 들어가지만 운영 서버다)
    //  - debug   : 기본값은 release 와 동일. local.properties 의 checkin.baseUrl 로 로컬 서버를 볼 수 있다.
    // ⚠️ api.bodyswitch.co.kr 은 admin 앱으로 라우팅되어 /kiosk/** 가 전부 403이다. 바꾸지 말 것.
    private val BASE_URL = BuildConfig.BASE_URL

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

    // retry가 바깥, adminToken이 안쪽이다. 재시도할 때마다 토큰 갱신 여부를 다시 판단한다.
    @Provides
    @Singleton
    fun provideOkHttpClient(
        retryInterceptor: RetryInterceptor,
        adminTokenInterceptor: AdminTokenInterceptor,
    ): OkHttpClient =
        newOkHttpBuilder()
            .addInterceptor(retryInterceptor)
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
