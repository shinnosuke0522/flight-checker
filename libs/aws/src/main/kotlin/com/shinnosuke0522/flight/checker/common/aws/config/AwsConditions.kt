package com.shinnosuke0522.flight.checker.common.aws.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnBooleanProperty(value = [AwsConfigConstants.PROPERTY_ENABLED], havingValue = true, matchIfMissing = false)
annotation class ConditionalOnAwsEnabled

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnBooleanProperty(value = [AwsConfigConstants.PROPERTY_ENABLED], havingValue = false, matchIfMissing = true)
annotation class ConditionalOnAwsDisabled
