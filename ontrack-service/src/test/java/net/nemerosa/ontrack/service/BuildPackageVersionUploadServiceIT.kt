package net.nemerosa.ontrack.service

import net.nemerosa.ontrack.it.AbstractDSLTestSupport
import net.nemerosa.ontrack.model.security.BuildConfig
import net.nemerosa.ontrack.model.structure.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuildPackageVersionUploadServiceIT : AbstractDSLTestSupport() {

    @Autowired
    private lateinit var buildPackageVersionService: BuildPackageVersionService

    @Autowired
    private lateinit var buildPackageVersionUploadService: BuildPackageVersionUploadService

    @Test
    fun `Upload of packages`() {
        // Project and build acting as a reference
        val ref: Build = project<Build> {
            packageIds {
                test("net.nemerosa.ontrack:ontrack-model")
            }
            branch<Build> {
                build("3.38.5")
            }
        }
        // Source project
        project {
            branch {
                // Creates a build...
                val build = build()
                // ... and uploads some package versions
                build.uploadPackageVersions(
                        // Matching dependency
                        testPackageVersion("net.nemerosa.ontrack:ontrack-model", "3.38.5"),
                        // Unmatching dependency
                        testPackageVersion("org.apache.commons:commons-lang", "3.8.1")
                )
                // Gets the package versions
                val packageVersions = build.packageVersions
                assertEquals(2, packageVersions.size)
                // Matching
                val matching = packageVersions[0]
                assertEquals("net.nemerosa.ontrack:ontrack-model", matching.packageVersion.packageId.id)
                assertEquals("3.38.5", matching.packageVersion.version)
                assertEquals(ref.id(), matching.target?.id())
                // Unmatching
                val unmatching = packageVersions[1]
                assertEquals("org.apache.commons:commons-lang", unmatching.packageVersion.packageId.id)
                assertEquals("3.8.1", unmatching.packageVersion.version)
                assertNull(unmatching.target)
            }
        }
    }

    private fun testPackageVersion(id: String, version: String): PackageVersion =
            testPackageId(id).toVersion(version)

    /**
     * Uploading some versions for a build
     */
    private fun Build.uploadPackageVersions(vararg packages: PackageVersion) {
        asUser().with(this, BuildConfig::class.java).execute {
            buildPackageVersionUploadService.uploadAndResolvePackageVersions(
                    this,
                    packages.toList()
            )
        }
    }

    /**
     * Gets the packages associated with a build
     */
    private val Build.packageVersions: List<BuildPackageVersion>
        get() =
            buildPackageVersionService.getBuildPackages(this)


}