package com.bodyswitch.checkin.data.api

import com.bodyswitch.checkin.data.api.dto.AdminLoginRequest
import com.bodyswitch.checkin.data.api.dto.AdminLoginResponse
import com.bodyswitch.checkin.data.api.dto.AttendanceResponse
import com.bodyswitch.checkin.data.api.dto.BranchInfoResponse
import com.bodyswitch.checkin.data.api.dto.CancelCheckinRequest
import com.bodyswitch.checkin.data.api.dto.CancelCheckinResponse
import com.bodyswitch.checkin.data.api.dto.CheckinHistoryResponse
import com.bodyswitch.checkin.data.api.dto.AttendRequest
import com.bodyswitch.checkin.data.api.dto.AttendResponse
import com.bodyswitch.checkin.data.api.dto.CheckinRequest
import com.bodyswitch.checkin.data.api.dto.CheckinResponse
import com.bodyswitch.checkin.data.api.dto.DoorListResponse
import com.bodyswitch.checkin.data.api.dto.OpenDoorRequest
import com.bodyswitch.checkin.data.api.dto.ReentryRequest
import com.bodyswitch.checkin.data.api.dto.ReentryResponse
import com.bodyswitch.checkin.data.api.dto.EmployeeAttendHistoryResponse
import com.bodyswitch.checkin.data.api.dto.EmployeeCheckinRequest
import com.bodyswitch.checkin.data.api.dto.EmployeeCheckinResponse
import com.bodyswitch.checkin.data.api.dto.StaffCallRequest
import com.bodyswitch.checkin.data.api.dto.StaffCallResponse
import com.bodyswitch.checkin.data.api.dto.PhoneLoginRequest
import com.bodyswitch.checkin.data.api.dto.ReservationsResponse
import com.bodyswitch.checkin.data.api.dto.QrLoginRequest
import com.bodyswitch.checkin.data.api.dto.QrLoginResponse
import com.bodyswitch.checkin.data.api.dto.UidLoginRequest
import com.bodyswitch.checkin.data.api.dto.TicketsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface KioskApi {

    // 관리자 로그인
    @POST("kiosk/api/v1/admin/login")
    suspend fun adminLogin(@Body request: AdminLoginRequest): AdminLoginResponse

    // 지점 정보 조회
    @GET("kiosk/api/v1/admin/branch-info")
    suspend fun getBranchInfo(
        @Header("Authorization") authorization: String,
    ): BranchInfoResponse

    // 회원 QR 로그인
    @POST("kiosk/api/v1/auth/qr-login")
    suspend fun qrLogin(
        @Header("X-Admin-Token") adminToken: String? = null,
        @Body request: QrLoginRequest,
    ): QrLoginResponse

    // 회원 전화번호 로그인
    @POST("kiosk/api/v1/auth/phone-login")
    suspend fun phoneLogin(
        @Header("X-Admin-Token") adminToken: String? = null,
        @Body request: PhoneLoginRequest,
    ): QrLoginResponse

    // 회원 userId 로그인
    @POST("kiosk/api/v1/auth/uid-login")
    suspend fun uidLogin(
        @Header("X-Admin-Token") adminToken: String? = null,
        @Body request: UidLoginRequest,
    ): QrLoginResponse

    // 이용권 목록 조회
    @GET("kiosk/api/v1/checkin/tickets")
    suspend fun getTickets(
        @Header("Authorization") authorization: String,
        @Query("branchId") branchId: Long? = null,
    ): TicketsResponse

    // 체크인 차감
    @POST("kiosk/api/v1/checkin")
    suspend fun checkin(
        @Header("Authorization") authorization: String,
        @Body request: CheckinRequest,
    ): Response<CheckinResponse>

    // 지점 체크인 이력 조회
    @GET("kiosk/api/v1/checkin/history")
    suspend fun getCheckinHistory(
        @Header("Authorization") authorization: String,
        @Query("branchId") branchId: Long? = null,
        @Query("searchInput") searchInput: String? = null,
        @Query("status") status: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): CheckinHistoryResponse

    // 지점 출석 기록 조회
    @GET("kiosk/api/v1/checkin/attendance")
    suspend fun getAttendance(
        @Header("Authorization") authorization: String,
        @Query("branchId") branchId: Long? = null,
        @Query("searchInput") searchInput: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): AttendanceResponse

    // 오늘 예약 수업 조회
    @GET("kiosk/api/v1/checkin/reservations")
    suspend fun getReservations(
        @Header("Authorization") authorization: String,
        @Query("branchId") branchId: Long,
        @Query("ticketType") ticketType: String,
        @Query("ticketId") ticketId: Long,
    ): ReservationsResponse

    // 예약 기반 출석 처리
    @POST("kiosk/api/v1/checkin/attend")
    suspend fun attend(
        @Header("Authorization") authorization: String,
        @Header("X-Admin-Token") adminToken: String?,
        @Body request: AttendRequest,
    ): AttendResponse

    // 무차감 재입장 (당일 출석 회원)
    @POST("kiosk/api/v1/checkin/reentry")
    suspend fun reentry(
        @Header("Authorization") authorization: String,
        @Header("X-Admin-Token") adminToken: String?,
        @Body request: ReentryRequest,
    ): ReentryResponse

    // 체크인 취소 (관리자)
    @POST("kiosk/api/v1/checkin/cancel")
    suspend fun cancelCheckin(
        @Header("Authorization") authorization: String,
        @Header("X-Admin-Token") adminToken: String,
        @Body request: CancelCheckinRequest,
    ): CancelCheckinResponse

    // 직원 체크인 (출퇴근 기록)
    @POST("kiosk/api/v1/checkin/employee")
    suspend fun employeeCheckin(
        @Header("Authorization") authorization: String,
        @Body request: EmployeeCheckinRequest,
    ): EmployeeCheckinResponse

    // 직원 출입 기록 조회
    @GET("kiosk/api/v1/checkin/employee/attend-histories")
    suspend fun getEmployeeAttendHistories(
        @Header("Authorization") authorization: String,
        @Query("branchId") branchId: Long,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("searchInput") searchInput: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): EmployeeAttendHistoryResponse

    // 직원 호출 알림톡 발송

    @POST("kiosk/api/v1/branches/manager/call")
    suspend fun staffCall(
        @Header("Authorization") authorization: String,
        @Body request: StaffCallRequest,
    ): StaffCallResponse

    // 지점 출입문 목록 조회 (출입문 설정 화면). 미연동 지점은 connected=false
    @GET("kiosk/api/v1/iot/doors")
    suspend fun getDoors(
        @Header("Authorization") authorization: String,
        @Query("branchId") branchId: Long,
    ): DoorListResponse

    // 출입문 열기 (체크인 성공 후 best-effort)
    @POST("kiosk/api/v1/iot/door/open")
    suspend fun openDoor(
        @Header("Authorization") authorization: String,
        @Body request: OpenDoorRequest,
    ): Response<Unit>
}
