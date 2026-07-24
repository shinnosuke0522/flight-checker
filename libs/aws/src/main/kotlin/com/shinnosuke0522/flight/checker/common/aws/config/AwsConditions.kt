package com.shinnosuke0522.flight.checker.common.aws.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnBooleanProperty(value = [AwsConfigConstants.PROPERTY_ENABLED], havingValue = true)
annotation class ConditionalOnAwsEnabled

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnBooleanProperty(value = [AwsConfigConstants.PROPERTY_ENABLED], havingValue = false)
annotation class ConditionalOnAwsDisabled
