plugins {
    id("org.openapi.generator") version "7.8.0"
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":domain"))
    implementation(libs.spring.boot.starter.restclient)
    implementation(libs.spring.boot.starter.webflux)
    // OpenAPI Generatorが自動生成するコード（モデル等）に付与される@Schemaなどのアノテーションを解決するため
    implementation(libs.swagger.annotations)
    implementation(libs.okhttp)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlinx.datetime)
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-okhttp4")
    validateSpec.set(false)
    inputSpec.set("$projectDir/src/main/resources/contract/aerodatabox-api-v1.15.1.0.yaml")
    outputDir.set("${layout.buildDirectory.get().asFile.path}/generated/openapi")
    apiPackage.set("com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api")
    modelPackage.set("com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model")

    configOptions.put("useSpringBoot3", "true")
    configOptions.put("dateLibrary", "kotlinx-datetime")
    configOptions.put("serializationLibrary", "jackson")

    generateApiTests.set(false)
    generateModelTests.set(false)

}

tasks.named("openApiGenerate") {
    doLast {
        val apiClient = file("${layout.buildDirectory.get().asFile.path}/generated/openapi/src/main/kotlin/org/openapitools/client/infrastructure/ApiClient.kt")
        if (apiClient.exists()) {
            val content = apiClient.readText()
            apiClient.writeText(content.replace("parseDateToQueryString(value)", "parseDateToQueryString(value as Any)"))
        }
    }
}

sourceSets {
    main {
        kotlin.srcDir("${layout.buildDirectory.get().asFile.path}/generated/openapi/src/main/kotlin")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(tasks.openApiGenerate)
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}
