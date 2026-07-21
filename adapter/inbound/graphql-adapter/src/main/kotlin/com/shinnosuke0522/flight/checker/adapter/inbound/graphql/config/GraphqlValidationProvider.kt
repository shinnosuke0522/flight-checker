package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.config

import graphql.validation.rules.OnValidationErrorStrategy
import graphql.validation.rules.ValidationRules
import graphql.validation.schemawiring.ValidationSchemaWiring

object GraphqlValidationProvider {

    fun validationSchemaWiring(): ValidationSchemaWiring {
        val validationRules = ValidationRules.newValidationRules()
            .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
            .build()

        return ValidationSchemaWiring(validationRules)
    }
}
