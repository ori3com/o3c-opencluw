package cloud.ori3com.o3clu.backend

object AgentClientFactory {
    fun create(config: AgentBackendConfig): AgentClient = when (config.type) {
        BackendType.HERMES_API_SERVER -> HermesApiServerClient(config)
        BackendType.OPENCLAW_GATEWAY -> OpenClawGatewayAdapter(config)
        BackendType.OPENCLAW_HTTP -> OpenClawHttpAdapter(config)
    }
}
