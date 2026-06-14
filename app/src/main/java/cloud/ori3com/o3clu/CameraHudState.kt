package cloud.ori3com.o3clu

enum class CameraHudKind {
    Photo,
    Recording,
    Success,
    Error,
}

data class CameraHudState(
    val token: Long,
    val kind: CameraHudKind,
    val message: String,
)
