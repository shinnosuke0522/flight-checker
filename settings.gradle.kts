rootProject.name = "flight-checker"

include(":domain")
include(":adapter:inbound:graphql-adaptor")
include(":adapter:outbound:dynamodb-adapter")
include(":libs:aws")
