package net.nemerosa.ontrack.bdd.model

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["net.nemerosa.ontrack.bdd"])
@EnableConfigurationProperties(BDDProperties::class)
class BDDConfig
