package net.nemerosa.ontrack.extension.general

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessagePropertyGraphQLIT : AbstractGeneralExtensionTestSupport() {

    @Test
    fun `Getting the message property on an entity`() {
        project {
            branch {
                // Sets the property
                message("This is a message")
                // Gets the property back using GraphQL
                val data = run("""{ branches(id: $id) {
                        properties(type: "${MessagePropertyType::class.java.name}") {
                            value
                        }
                    }}""")
                // Checks the value
                val p = data["branches"][0]["properties"][0]
                val value = p["value"]
                assertEquals("This is a message", value["text"].asText())
                assertEquals("INFO", value["type"].asText())
            }
        }
    }

    @Test
    fun `Getting the message property on an entity using native field`() {
        project {
            branch {
                // Sets the property
                message("This is a message")
                // Gets the property back using GraphQL
                val data = run("""{ branches(id: $id) {
                        messageProperty {
                            type
                            text
                        }
                    }}""")
                // Checks the value
                val p = data["branches"][0]["messageProperty"]
                assertEquals("This is a message", p["text"].asText())
                assertEquals("INFO", p["type"].asText())
            }
        }
    }

    @Test
    fun `Setting the message property using a mutation`() {
        project {
            branch {
                // Sets the property using a mutation
                run("""
                    mutation SetMessage {
                        messageProperty(
                            entity: {type: "BRANCH", id: $id},
                            value: {type: WARNING, text: "This is my new message"}
                        ) {
                            id
                        }
                    }
                """)
                // Checks the result
                val message = getProperty(this, MessagePropertyType::class.java)
                assertNotNull(message) {
                    assertEquals(MessageType.WARNING, it.type)
                    assertEquals("This is my new message", it.text)
                }
            }
        }
    }

    @Test
    fun `Setting the message property using a mutation and default message type`() {
        project {
            branch {
                // Sets the property using a mutation
                run("""
                    mutation SetMessage {
                        messageProperty(
                            entity: {type: "BRANCH", id: $id},
                            value: {text: "This is my new message"}
                        ) {
                            id
                        }
                    }
                """)
                // Checks the result
                val message = getProperty(this, MessagePropertyType::class.java)
                assertNotNull(message) {
                    assertEquals(MessageType.INFO, it.type)
                    assertEquals("This is my new message", it.text)
                }
            }
        }
    }

}