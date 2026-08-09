rootProject.name = "flight-checker"

include(":domain")
include(":adapter:inbound:graphql-adapter")
include(":adapter:outbound:dynamodb-adapter")
include(":adapter:outbound:firestore-adapter")
include(":adapter:outbound:aerodatabox-api-adapter")
include(":libs:aws")
