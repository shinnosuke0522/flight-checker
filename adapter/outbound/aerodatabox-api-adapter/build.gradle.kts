plugins {
    id("org.openapi.generator") version "7.8.0"
    alias(libs.plugins.quarkus.plugin)
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(project(":domain"))
    // OpenAPI Generatorが自動生成するコード（モデル等）に付与される@Schemaなどのアノテーションを解決するため
    implementation(libs.swagger.annotations)
    implementation(libs.okhttp)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Quarkus
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.jackson)
    implementation(libs.quarkus.rest.client.jackson)
    // For Test
    listOf(
        "testImplementation",
        "integrationTestImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, platform(libs.kotest.bom))
        add(configuration, libs.bundles.quarkus.test.core)
    }
    listOf(
        "integrationTestImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, libs.wiremock)
        add(configuration, libs.openapi.validator.wiremock)
    }

    // Quarkus Test
    testImplementation(libs.quarkus.junit5)
    integrationTestImplementation(libs.quarkus.junit5)
    testFixturesImplementation(libs.quarkus.junit5)

}

configurations.all {
    exclude(group = "com.github.tomakehurst", module = "wiremock-jre8")
    exclude(group = "org.eclipse.jetty", module = "jetty-alpn-openjdk8-client")
    exclude(group = "org.eclipse.jetty", module = "jetty-alpn-openjdk8-server")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.named("test"))
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-okhttp4")
    validateSpec.set(false)
    inputSpec.set("$projectDir/src/main/resources/contract/aerodatabox-api-v1.15.1.0.yaml")
    outputDir.set("${layout.buildDirectory.get().asFile.path}/generated/openapi")
    apiPackage.set("com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.api")
    modelPackage.set("com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.model")
    apiNameSuffix.set("Client")

    configOptions.put("useJakartaEe", "true")
    configOptions.put("dateLibrary", "java8")
    configOptions.put("serializationLibrary", "jackson")

    globalProperties.set(mapOf(
        "models" to "",
        "apis" to "false",
        "supportingFiles" to "false"
    ))
    
    generateApiTests.set(false)
    generateModelTests.set(false)
}

tasks.named("openApiGenerate") {
    val buildDirPath = layout.buildDirectory.get().asFile.path
    doLast {
        val apiClient = file(
            path = "$buildDirPath/generated/openapi/src/main/kotlin/org/openapitools/client/infrastructure/ApiClient.kt"
        )
        if (apiClient.exists()) {
            val content = apiClient.readText()
            apiClient.writeText(
                content.replace(
                    oldValue = "parseDateToQueryString(value)",
                    newValue = "parseDateToQueryString(value as Any)"
                )
            )
        }

        // Rename *Api to *Client
        val apiDir = file(
            path = "$buildDirPath/generated/openapi/src/main/kotlin/"
                    + "com/shinnosuke0522/flight/checker/adapter/outbound/aerodatabox/api"
        )
        if (apiDir.exists()) {
            apiDir.listFiles()?.forEach { f ->
                if (f.name.endsWith("Api.kt")) {
                    val newName = f.name.replace("Api.kt", "Client.kt")
                    val oldClassName = f.nameWithoutExtension
                    val newClassName = newName.removeSuffix(".kt")
                    val content = f.readText().replace(oldClassName, newClassName)
                    f.writeText(content)
                    f.renameTo(File(apiDir, newName))
                }
            }
        }

        // 警告抑止の追加 (RedundantCallOfConversionMethod, DEPRECATION)
        val generatedSrcDir = file("$buildDirPath/generated/openapi/src/main/kotlin")
        if (generatedSrcDir.exists()) {
            generatedSrcDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { ktFile ->
                val content = ktFile.readText()
                if (content.contains("@file:Suppress(\n")) {
                    if (!content.contains("\"RedundantCallOfConversionMethod\"")) {
                        ktFile.writeText(
                            content.replace(
                                "@file:Suppress(\n",
                                "@file:Suppress(\n    \"RedundantCallOfConversionMethod\",\n    \"DEPRECATION\",\n"
                            )
                        )
                    }
                }
            }
        }
    }
}

sourceSets.getByName("main") {
    java.srcDir("${layout.buildDirectory.get().asFile.path}/generated/openapi/src/main/kotlin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(tasks.openApiGenerate)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        if (name == "compileKotlin") {
            freeCompilerArgs.add("-nowarn")
        }
    }
}



tasks.jar {
    enabled = true
}
