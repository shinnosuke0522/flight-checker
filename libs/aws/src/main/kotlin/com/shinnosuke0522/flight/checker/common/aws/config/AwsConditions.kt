package com.shinnosuke0522.flight.checker.common.aws.config

import io.quarkus.arc.properties.IfBuildProperty

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@IfBuildProperty(name = AwsConfigConstants.PROPERTY_ENABLED, stringValue = "true")
annotation class ConditionalOnAwsEnabled

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@IfBuildProperty(name = AwsConfigConstants.PROPERTY_ENABLED, stringValue = "false", enableIfMissing = true)
annotation class ConditionalOnAwsDisabled
