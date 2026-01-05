package au.com.gman.bottlerocket.domain

enum class ApiStatusCodeEnum(val code: Int) {
    OK(0),
    UNKNOWN_ERROR(1000),
    NO_ATTACHMENTS_FOUND(1001)
}