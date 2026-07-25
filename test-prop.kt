import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty

@ConditionalOnBooleanProperty(value = ["test"], havingValue = false, matchIfMissing = true)
class TestClass
