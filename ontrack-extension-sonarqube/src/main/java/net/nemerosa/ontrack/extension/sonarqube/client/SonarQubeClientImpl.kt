package net.nemerosa.ontrack.extension.sonarqube.client

import net.nemerosa.ontrack.extension.sonarqube.client.model.*
import net.nemerosa.ontrack.extension.sonarqube.configuration.SonarQubeConfiguration
import net.nemerosa.ontrack.json.asJson
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

class SonarQubeClientImpl(
        configuration: SonarQubeConfiguration
) : SonarQubeClient {

    private val logger = LoggerFactory.getLogger(SonarQubeClient::class.java)

    override val serverVersion: String
        get() = restTemplate.getForObject("/api/server/version", String::class.java)

    override val systemHealth: String
        get() = restTemplate.getForObject("/api/system/health", SystemHealth::class.java).health

    override fun getMeasuresForVersion(key: String, branch: String, version: String, metrics: List<String>): Map<String, Double?>? {

        if (logger.isDebugEnabled) {
            logger.debug("Getting measures,key=$key,branch=$branch,version=$version,metrics=$metrics")
        }

        val analysis: Analysis? = paginateUntil(
                ProjectAnalysisSearch::class.java,
                uri = { page ->
                    "/api/project_analyses/search?project={project}&branch={branch}&category=VERSION&p={page}" to mapOf(
                            "project" to key,
                            "branch" to branch,
                            "page" to page
                    )
                },
                search = { result ->
                    result.analyses.find { analysis ->
                        analysis.events.any { event -> event.name == version }
                    }
                }
        )

        if (analysis != null) {
            if (logger.isDebugEnabled) {
                logger.debug("Getting analysis,key=$key,branch=$branch,version=$version,metrics=$metrics,analysis=${analysis.asJson()}")
            }
            // Timestamp of the version
            val timestamp = analysis.date
            // History measures
            val measures: MeasureSearchHistory = restTemplate.getForObject(
                    "/api/measures/search_history?component={component}&branch={branch}&metrics={metrics}&from={timestamp}&to={timestamp}",
                    MeasureSearchHistory::class.java,
                    mapOf(
                            "component" to key,
                            "branch" to branch,
                            "metrics" to metrics.joinToString(","),
                            "timestamp" to timestamp
                    )
            )
            if (logger.isDebugEnabled) {
                logger.debug("Getting measures,key=$key,branch=$branch,version=$version,metrics=$metrics,measures=${measures.measures.asJson()}")
            }
            // Converts to measures
            val finalMeasures = measures.measures.associate { measure ->
                val name = measure.metric
                val value = measure.history.firstOrNull()?.value
                if (value != null) {
                    try {
                        name to value.toDouble()
                    } catch (ex: NumberFormatException) {
                        name to null
                    }
                } else {
                    name to null
                }
            }
            if (logger.isDebugEnabled) {
                logger.debug("Final measures,key=$key,branch=$branch,version=$version,metrics=$metrics,measures=${finalMeasures.asJson()}")
            }
            return finalMeasures
        } else {
            return null
        }

    }

    private val restTemplate: RestTemplate = RestTemplateBuilder()
            .rootUri(configuration.url)
            .basicAuthorization(configuration.password, "") // See https://docs.sonarqube.org/latest/extend/web-api/
            .build()

    private fun <T, R : PagedResult> paginateUntil(
            resultType: Class<R>,
            uri: (page: Int) -> Pair<String, Map<String, Any>>,
            search: (result: R) -> T?
    ): T? {
        var page = 1
        var result: T? = null
        while (result == null) {
            // URI to call
            val url = uri(page++)
            // Getting the page
            val pageResult: R? = try {
                restTemplate.getForObject(url.first, resultType, url.second)
            } catch (ex: HttpClientErrorException) {
                if (ex.statusCode == HttpStatus.NOT_FOUND) {
                    null
                } else {
                    throw ex
                }
            }
            // Empty results?
            if (pageResult == null || pageResult.isEmpty) {
                return null
            }
            // Gets a result in there
            result = search(pageResult)
        }
        // Result
        return result
    }

}