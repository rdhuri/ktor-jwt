package se.zensum.jwt

import com.auth0.jwt.interfaces.DecodedJWT
import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.application.ApplicationCallPipeline
import org.jetbrains.ktor.application.ApplicationFeature
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.request.ApplicationRequest
import org.jetbrains.ktor.request.path
import org.jetbrains.ktor.util.AttributeKey
import org.jetbrains.ktor.util.ValuesMap

private val REQUEST_KEY = AttributeKey<DecodedJWT>("jwt")

class JWTFeature(private val config: JWTConfig) {

    private suspend fun intercept(context: PipelineContext<Unit>) {
        val headers: ValuesMap = context.call.request.headers
        val path: String = context.call.request.path()
        verifyToken(config, headers, path)?.let {
            context.call.attributes.put(REQUEST_KEY, it)
        }
    }

    companion object Feature: ApplicationFeature<ApplicationCallPipeline, JWTConfig, JWTFeature> {
        override val key: AttributeKey<JWTFeature> = AttributeKey("JWT")
        override fun install(pipeline: ApplicationCallPipeline, configure: JWTConfig.() -> Unit): JWTFeature {
            val result = JWTFeature(JWTConfig().apply(configure))
            pipeline.intercept(ApplicationCallPipeline.Call) {
                result.intercept(this)
            }
            return result
        }
    }
}

fun PipelineContext<Unit>.isVerified(): Boolean = this.call.isVerified()
fun PipelineContext<Unit>.token(): DecodedJWT? = this.call.token()
fun ApplicationCall.isVerified(): Boolean = token() != null
fun ApplicationCall.token(): DecodedJWT? =
    if (REQUEST_KEY in attributes)
        attributes[REQUEST_KEY]
    else null