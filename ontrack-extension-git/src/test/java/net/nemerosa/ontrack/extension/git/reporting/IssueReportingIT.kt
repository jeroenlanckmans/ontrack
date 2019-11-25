package net.nemerosa.ontrack.extension.git.reporting

import net.nemerosa.ontrack.extension.git.AbstractGitTestSupport
import net.nemerosa.ontrack.extension.issues.support.MockIssue
import net.nemerosa.ontrack.extension.issues.support.MockIssueServiceExtension
import net.nemerosa.ontrack.extension.issues.support.MockIssueStatus
import net.nemerosa.ontrack.model.structure.Project
import net.nemerosa.ontrack.model.structure.ValidationRunStatusID
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

/**
 * We want to get the last open issues on a pipeline represented by a branch.
 *
 * Given the following situation:
 *
 * ```
 * | Build | VS0    | VS1                   | VS2                 |
 * |-------|--------|-----------------------|---------------------|
 * | 1.1   | Passed | Passed                | Failed with #4 open |
 * | 1.0   | Failed | Failed with #3 closed | Passed              |
 * ```
 */
class IssueReportingIT : AbstractGitTestSupport() {

    @Autowired
    private lateinit var mockIssueServiceExtension: MockIssueServiceExtension

    @Test
    fun `Getting last open issues on a branch`() {
        withTestContext { project ->
            // Running the query to get the opened issues
            val data = run("""{
                projects(id: ${project.id}) {
                    branches(name: "master") {
                        validationStamps {
                            name
                            validationRunsPaginated(size: 20, passed: false) {
                                pageItems {
                                    build {
                                        name
                                    }
                                    validationRunStatuses {
                                        statusID {
                                            id
                                        }
                                        issues(status: "OPEN") {
                                            key
                                            displayKey
                                            summary
                                            url
                                            status
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }""")
            val branch = data["projects"][0]["branches"][0]
            // Check issue #3 is not present
            branch.apply {
                val vs = get("validationStamps").first { it["name"].asText() == "VS1" }
                val run = vs["validationRunsPaginated"]["pageIteams"].first { it["build"]["name"].asText() == "1.0" }
                val status = run["validationRunStatuses"].first { it["statusID"]["id"].asText() == "DEFECTIVE" }
                val issues = status["issues"]
                assertEquals(0, issues.size()) // #3 is excluded by the filter
            }
            // Check issue #4 is present
            branch.apply {
                val vs = get("validationStamps").first { it["name"].asText() == "VS2" }
                val run = vs["validationRunsPaginated"]["pageIteams"].first { it["build"]["name"].asText() == "1.1" }
                val status = run["validationRunStatuses"].first { it["statusID"]["id"].asText() == "DEFECTIVE" }
                val issues = status["issues"]
                assertEquals(1, issues.size())
                val issue = issues[0]
                assertEquals(4, issue["key"].asInt())
                assertEquals("#4", issue["displayKey"].asText())
                assertEquals("Issue #4", issue["summary"].asText())
                assertEquals("", issue["url"].asText())
                assertEquals("OPEN", issue["status"].asText())
            }
        }
    }

    @Test
    fun `Getting last issues on a branch`() {
        withTestContext { project ->
            // Running the query to get the opened issues
            val data = run("""{
                projects(id: ${project.id}) {
                    branches(name: "master") {
                        validationStamps {
                            name
                            validationRunsPaginated(size: 20, passed: false) {
                                pageItems {
                                    build {
                                        name
                                    }
                                    validationRunStatuses {
                                        statusID {
                                            id
                                        }
                                        issues {
                                            key
                                            displayKey
                                            summary
                                            url
                                            status
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }""")
            val branch = data["projects"][0]["branches"][0]
            // Check issue #3 is not present
            branch.apply {
                val vs = get("validationStamps").first { it["name"].asText() == "VS1" }
                val run = vs["validationRunsPaginated"]["pageIteams"].first { it["build"]["name"].asText() == "1.0" }
                val status = run["validationRunStatuses"].first { it["statusID"]["id"].asText() == "DEFECTIVE" }
                val issues = status["issues"]
                assertEquals(1, issues.size())
                val issue = issues[0]
                assertEquals(3, issue["key"].asInt())
                assertEquals("#3", issue["displayKey"].asText())
                assertEquals("Issue #3", issue["summary"].asText())
                assertEquals("", issue["url"].asText())
                assertEquals("CLOSED", issue["status"].asText())
            }
            // Check issue #4 is present
            branch.apply {
                val vs = get("validationStamps").first { it["name"].asText() == "VS2" }
                val run = vs["validationRunsPaginated"]["pageIteams"].first { it["build"]["name"].asText() == "1.1" }
                val status = run["validationRunStatuses"].first { it["statusID"]["id"].asText() == "DEFECTIVE" }
                val issues = status["issues"]
                assertEquals(1, issues.size())
                val issue = issues[0]
                assertEquals(4, issue["key"].asInt())
                assertEquals("#4", issue["displayKey"].asText())
                assertEquals("Issue #4", issue["summary"].asText())
                assertEquals("", issue["url"].asText())
                assertEquals("OPEN", issue["status"].asText())
            }
        }
    }

    private fun withTestContext(code: (Project) -> Unit) {
        createRepo {
            commits(1)
        } and { repo, _ ->
            // Registers the issues
            mockIssueServiceExtension.resetIssues()
            mockIssueServiceExtension.register(
                    MockIssue(3, MockIssueStatus.CLOSED, "bug"),
                    MockIssue(4, MockIssueStatus.OPEN, "bug")
            )
            // Project & branch
            val project = project {
                gitProject(repo)
                branch("master") {
                    gitBranch {
                        commitAsProperty()
                    }
                    // Creates all the validations we want to monitor
                    val stamps = (0..2).map { validationStamp("VS$it") }
                    // Creates a list of builds
                    val builds = (1 downTo 0).map { build("1.$it") }
                    // Activates the validations & descriptions
                    builds[0].validate(stamps[0], ValidationRunStatusID.STATUS_FAILED)
                    builds[0].validate(stamps[1], ValidationRunStatusID.STATUS_FAILED)
                            .validationStatus(ValidationRunStatusID.STATUS_DEFECTIVE, "Issue #3 will be closed")
                    builds[0].validate(stamps[2], ValidationRunStatusID.STATUS_PASSED)
                    builds[1].validate(stamps[0], ValidationRunStatusID.STATUS_PASSED)
                    builds[1].validate(stamps[1], ValidationRunStatusID.STATUS_PASSED)
                    builds[1].validate(stamps[2], ValidationRunStatusID.STATUS_FAILED)
                            .validationStatus(ValidationRunStatusID.STATUS_DEFECTIVE, "Issue #4 will remain opened")
                }
            }
            // Running the test
            code(project)
        }
    }

}