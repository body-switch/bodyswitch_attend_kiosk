package com.bodyswitch.checkin.data.session

object EmployeeLoginHolder {
    var token: String? = null
    var branchId: Long? = null
    var employeeName: String? = null
    var checkInMethod: String? = null

    fun set(token: String, branchId: Long, employeeName: String, checkInMethod: String) {
        this.token = token
        this.branchId = branchId
        this.employeeName = employeeName
        this.checkInMethod = checkInMethod
    }

    fun clear() {
        token = null
        branchId = null
        employeeName = null
        checkInMethod = null
    }
}
