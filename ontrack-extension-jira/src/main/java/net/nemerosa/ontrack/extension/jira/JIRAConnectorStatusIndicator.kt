package net.nemerosa.ontrack.extension.jira

import net.nemerosa.ontrack.extension.jira.tx.JIRASessionFactory
import net.nemerosa.ontrack.extension.support.ConfigurationConnectorStatusIndicator
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.support.ConfigurationService
import net.nemerosa.ontrack.model.support.Connector
import net.nemerosa.ontrack.model.support.ConnectorDescription
import org.springframework.stereotype.Component

@Component
class JIRAConnectorStatusIndicator(
        configurationService: ConfigurationService<JIRAConfiguration>,
        securityService: SecurityService,
        private val sessionFactory: JIRASessionFactory
) : ConfigurationConnectorStatusIndicator<JIRAConfiguration>(configurationService, securityService) {

    override val type: String = "jira"

    override fun connect(config: JIRAConfiguration) {
        sessionFactory.create(config).client.projects
    }

    override fun connectorDescription(config: JIRAConfiguration) = ConnectorDescription(
            connector = Connector(type, config.name),
            connection = config.url
    )

}
