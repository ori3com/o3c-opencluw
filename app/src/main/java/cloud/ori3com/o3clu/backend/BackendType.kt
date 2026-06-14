package cloud.ori3com.o3clu.backend

import kotlinx.serialization.Serializable

@Serializable
enum class BackendType {
    OPENCLAW_GATEWAY,
    OPENCLAW_HTTP,
    HERMES_API_SERVER,
}
