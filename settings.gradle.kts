rootProject.name = "flight-checker"

include(":domain")
include(":adapter:inbound:graphql-adapter")
include(":adapter:outbound:dynamodb-adapter")
include(":libs:aws")
