package net.nemerosa.ontrack.extension.scm.relnotes

import net.nemerosa.ontrack.common.Document
import net.nemerosa.ontrack.common.getOrNull
import net.nemerosa.ontrack.extension.api.ExtensionManager
import net.nemerosa.ontrack.extension.api.model.IssueChangeLogExportRequest
import net.nemerosa.ontrack.extension.issues.export.IssueExportService
import net.nemerosa.ontrack.extension.issues.export.IssueExportServiceFactory
import net.nemerosa.ontrack.extension.scm.service.SCMService
import net.nemerosa.ontrack.model.buildfilter.BuildFilterService
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.Project
import net.nemerosa.ontrack.model.structure.StructureService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// TODO Metrics

@Service
@Transactional
class ReleaseNotesServiceImpl(
        private val extensionManager: ExtensionManager,
        private val structureService: StructureService,
        private val scmService: SCMService,
        private val buildFilterService: BuildFilterService,
        private val issueExportServiceFactory: IssueExportServiceFactory
) : ReleaseNotesService {

    override fun exportProjectReleaseNotes(project: Project, request: ReleaseNotesRequest): Document {
        // Structured release notes
        val releaseNotes = getProjectReleaseNotes(project, request)
        // Gets the export service
        val exportService: IssueExportService = issueExportServiceFactory.getIssueExportService(request.format)
        // Exporting
        return exportService.exportReleaseNotes(releaseNotes)
    }

    override fun getProjectReleaseNotes(project: Project, request: ReleaseNotesRequest): ReleaseNotes {
        // Gets all the generation extensions
        // Filter on the project and gets the first one
        // ... at least one is required
        val generation: ReleaseNotesGenerationExtension =
                extensionManager.getExtensions(ReleaseNotesGenerationExtension::class.java)
                        .firstOrNull { it.appliesForProject(project) }
                        ?: throw ReleaseNotesGenerationExtensionMissingException(project.name)
        // Gets all the branches of this project
        // gets the path definition for this branch (ex. Git branch)
        // and filters them based on the request branch pattern
        val branchRegex = request.branchPattern.toRegex()
        val branches = structureService.getBranchesForProject(project.id)
                .filter { branch ->
                    scmService.getSCMPathInfo(branch).getOrNull()?.branch?.let { path ->
                        branchRegex.matches(path)
                    } ?: false
                }
        // TODO Ordering of the branches
        // Getting all the builds of a given promotion (up to the limit)
        val builds = mutableListOf<Build>()
        branchLoop@ for (branch in branches) {
            // Gets all builds having a given promotion
            val branchBuilds = buildFilterService.standardFilterProviderData(request.buildLimit)
                    .withWithPromotionLevel(request.promotion)
                    .build()
                    .filterBranchBuilds(branch)
            // Filling the results
            for (build in branchBuilds) {
                if (builds.size < request.buildLimit) {
                    builds += build
                } else {
                    // Limit reached, exiting the loop
                    break@branchLoop
                }
            }
        }
        // For each build, get the change log since the previous one in the list
        val changeLogs = builds.mapIndexed { index, build ->
            if (index != builds.lastIndex) {
                val nextBuild = builds[index + 1]
                // Change log request
                val changeLogRequest = IssueChangeLogExportRequest().apply {
                    format = request.format
                    grouping = request.issueGrouping
                    exclude = request.issueExclude
                    altGroup = request.issueAltGroup
                    from = build.id
                    to = nextBuild.id
                }
                // Gets the change log
                val changeLog = generation.changeLog(changeLogRequest)
                // Association
                changeLog?.run { BuildChangeLog(build, this) }
            } else {
                null
            }
        }.filterNotNull().map { buildChangeLog ->
            ReleaseNotesVersion(
                    buildChangeLog.build,
                    buildChangeLog.changeLog
            )
        }
        // Grouping at branch level
        return if (request.branchGrouping.isBlank()) {
            // No grouping
            ReleaseNotes(
                    listOf(
                            ReleaseNotesGroup(
                                    null,
                                    changeLogs
                            )
                    )
            )
        } else {
            val groupRegex = request.branchGrouping.toRegex()
            ReleaseNotes(
                    changeLogs.groupBy { version ->
                        val versionBranch = version.build.branch
                        // Path
                        val path = scmService.getSCMPathInfo(versionBranch).orElse(null)?.branch!!
                        // Parsing
                        val m = groupRegex.matchEntire(path)
                                ?: throw ReleaseNotesBranchGroupingMismatchException(
                                        request.branchGrouping,
                                        versionBranch,
                                        path
                                )
                        if (m.groupValues.isEmpty()) {
                            throw ReleaseNotesBranchGroupingFormatException(request.branchGrouping)
                        } else {
                            m.groupValues[0]
                        }
                    }.map { (name, versions) ->
                        val groupTitle = String.format(request.branchGroupFormat, name)
                        ReleaseNotesGroup(
                                groupTitle,
                                versions
                        )
                    }
            )
        }
    }

    private class BuildChangeLog(
            val build: Build,
            val changeLog: String
    )

}