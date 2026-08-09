package com.shinnosuke0522.flight.checker.adapter.inbound.graphql.directive

import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

@GraphQLDirective(
    name = "Pattern",
    description = "Regex pattern validation",
    locations = [
        Introspection.DirectiveLocation.ARGUMENT_DEFINITION,
        Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION
    ]
)
annotation class Pattern(
    val regexp: String,
    val message: String = "graphql.validation.Pattern.message"
)

@GraphQLDirective(
    name = "Size",
    description = "Size validation",
    locations = [
        Introspection.DirectiveLocation.ARGUMENT_DEFINITION,
        Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION
    ]
)
annotation class Size(
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
    val message: String = "graphql.validation.Size.message"
)
