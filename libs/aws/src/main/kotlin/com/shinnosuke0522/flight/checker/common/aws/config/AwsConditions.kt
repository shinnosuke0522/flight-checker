package com.shinnosuke0522.flight.checker.common.aws.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnBooleanProperty(value = ["infrastructure.aws.enabled"], havingValue = true)
annotation class ConditionalOnAwsEnabled

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnBooleanProperty(value = ["infrastructure.aws.enabled"], havingValue = false)
annotation class ConditionalOnAwsDisabled
