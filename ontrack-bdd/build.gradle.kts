dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(project(":ontrack-bdd-engine"))
    implementation(project(":ontrack-kdsl-model"))

    implementation(project(":ontrack-extension-stale", "dsl"))
}
