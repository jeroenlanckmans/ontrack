package net.nemerosa.ontrack.graphql

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests around the `validationStamp` root query.
 */
class ValidationStampGraphQLIT : AbstractQLKTITSupport() {


    @Test
    fun `No validation stamp`() {
        val data = run("""{
            validationStamp(id: 1) {
                name
            }
        }""")

        val vs = data["validationStamp"]
        assertTrue(vs.isNull, "No validation stamp")
    }


    @Test
    fun `Validation stamp by ID`() {
        val vs = doCreateValidationStamp()
        val data = run("""{
            validationStamp(id: ${vs.id}) {
                name
            }
        }""")

        val name = data["validationStamp"]["name"].asText()
        assertEquals(vs.name, name)
    }

    @Test
    fun `Paginated list of validation runs`() {
        project {
            branch {
                // Creates a validation stamp
                val vs = validationStamp()
                // Creates 10 builds...
                repeat(10) { no ->
                    build("1.$no") {
                        // Validates N times the build
                        repeat(20) {
                            validate(vs)
                        }
                    }
                }
                // Checks the number of validation runs
                assertEquals(
                        200,
                        structureService.getValidationRunsCountForValidationStamp(vs.id),
                        "Checking the number of validation runs having been created"
                )
                // Paginated query with variables
                val query = """
                    query PaginatedValidationRuns(
                        ${'$'}validationStampId: Int!,
                        ${'$'}offset: Int = 0,
                        ${'$'}size: Int = 20) {
                        validationStamp(id: ${'$'}validationStampId) {
                            name
                            validationRunsPaginated(offset: ${'$'}offset, size: ${'$'}size) {
                                pageInfo {
                                    totalSize
                                    currentOffset
                                    currentSize
                                    previousPage {
                                        offset
                                        size
                                    }
                                    nextPage {
                                        offset
                                        size
                                    }
                                    pageIndex
                                    pageTotal
                                }
                                pageItems {
                                    id
                                    build {
                                        name
                                    }
                                }
                            }
                        }
                    }
                    """
                // Initial parameters
                val params = mutableMapOf(
                        "offset" to 0,
                        "size" to 20,
                        "validationStampId" to vs.id()
                )
                // Runs the query for the first page
                val data = run(query, params)
                // Checks the validation stamp info
                val vsNode = data["validationStamp"]
                assertEquals(vs.name, vsNode["name"].asText())
                // Gets the paginated list
                val paginated = vsNode["validationRunsPaginated"]
                val pageInfo = paginated["pageInfo"]
                val previousPage = pageInfo["previousPage"]
                val nextPage = pageInfo["nextPage"]
                assertEquals(200, pageInfo["totalSize"].asInt())
                assertEquals(0, pageInfo["currentOffset"].asInt())
                assertEquals(20, pageInfo["currentSize"].asInt())
                assertEquals(0, pageInfo["pageIndex"].asInt())
                assertEquals(10, pageInfo["pageTotal"].asInt())
                assertTrue(previousPage.isNull, "No previous page")
                assertFalse(nextPage.isNull, "There is a next page")
                assertEquals(20, nextPage["offset"].asInt())
                assertEquals(20, nextPage["size"].asInt())
            }
        }
    }

}