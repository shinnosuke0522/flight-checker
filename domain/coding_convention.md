# Coding Conventions

## Domain Layer 

### 1. 共通パターン

#### 1.1 関数型の設計思想 (Functional Programming)
*   **副作用の排除**: ドメイン層のロジックは副作用を持たない純粋関数として実装することを基本とする。
*   **関数の合成**: `arrow.core.Either` やコンピュテーションブロック (`either { ... }`) を活用し、安全かつ簡潔なロジックの合成を行う。

#### 1.2 Immutable Objectが基本
*   **完全なイミュータビリティ**: Entity, Value Object, Event を含むすべてのドメインモデルは、`val` プロパティのみを持つ `data class` または `value class` として定義する。
*   **意図せぬ変更の防止**: Kotlin の `copy` メソッドによる不整合を避けるため、必要に応じて `@ConsistentCopyVisibility` などの制限を検討する。

#### 1.3 不変条件 (Invariants) の担保の方針
*   **生成時の不変条件チェック**: ドメインオブジェクト（特に Value Object）は、生成時点で不正な状態を排除する。不変条件を満たさない状態のオブジェクトはシステム内に存在させてはならない。
*   **Smart Constructor**: `Companion.invoke` や専用の `create` メソッドを隠蔽されたコンストラクタ（`private constructor`）と組み合わせて使用し、不変条件チェックを経由しないインスタンス化を防ぐ。


#### 1.4 ExceptionではなくErrorベースのエラーハンドリング
*   **例外スローの禁止**: ドメインルールの違反や入力の不正に対しては、絶対に `throw Exception` を行わない。
*   **Eitherによる明示**: 失敗する可能性のある処理は、必ず戻り値の型として `Either<Error, Success>` を返す。これにより、エラーの可能性を型システムで強制的に処理させる。

---

### 2. Aggregate Root (集約ルート)

#### 2.1 ES + FP + OOP の思想に則った apply と replay の実装
*   **イベントソーシング (ES)**: 状態の変更は、直接プロパティを書き換えるのではなく、過去の事実である「イベント」を蓄積し、適用することで表現する。
*   **オブジェクト指向 (OOP) によるカプセル化**: 状態の適用ルールは集約内部にカプセル化する。`EventSourcingAggregateRoot` インターフェースを実装し、`apply(event: EVENT): SELF` を実装することによって「イベントを受けた新しい自身の状態（不変）」を返す。`apply` はドメインサービスから状態遷移を行うために `public` とする。
*   **状態の復元**: 過去のイベントのリスト（`NonEmptyList<EVENT>`）を受け取り、初期状態から順に `apply` を適用して現在の状態を復元する `Companion.replay` メソッドを実装する。

#### 2.2 不変条件の強制 (Factory Methodの徹底)
*   **copyメソッドの禁止**: 集約ルート（Aggregate Root）内では、Kotlin の `copy` メソッドの使用を禁止する。これは、`copy` がバリデーションをバイパスして不正な状態のオブジェクトを生成できてしまうためである。
*   **Factory Method経由の生成**: 状態の変更や生成は必ず `Companion.invoke` または専用のファクトリメソッド経由で行い、不変条件（Invariants）を常にチェックする。
*   **例外**: `DomainPrimitive` や、複雑な不変条件を持たない単純なバリューオブジェクトについては `copy` の使用を許容する。

#### 2.3 集約クラスの責務の純化
*   **状態と不変条件の保持**: 集約クラス自体の主な責務は、現在の状態の保持、生成時の不変条件のチェック、および `apply` による状態遷移ルール（イベントをどう状態に反映するか）の定義に限定する。
*   **振る舞いの分離**: 集約の状態を更新するビジネスロジック（操作メソッド）は、集約クラス内に実装せず、ドメインサービス層として分離する。

#### 2.4 サブタイプの活用による状態遷移の表現
*   **型の切り替え**: 集約のライフサイクルにおいて、状態（Status）の遷移によって保持すべきプロパティが大きく変わる場合や、許容されるビジネスロジックが変わる場合は、Enumによる分岐ではなく、Kotlinの `sealed interface` または `sealed class` を用いてサブタイプとして表現する。
    *(例: 未確定のフライト `ScheduledFlightInfo` と、遅延が確定した `DelayedFlightInfo` を型レベルで分けることで、不正な状態遷移をコンパイルエラーとして防ぐ。)*

#### 2.5 内部パーツのカプセル化と可視性
*   **集約ルート経由の操作**: 集約内部のパーツ（EntityやValue Objectのコレクション）に対する更新操作は、必ず集約ルートの `apply` または公開されたドメインサービスを経由して行う。これにより、集約全体の整合性とビジネスルールを維持する。
*   **更新系メソッドの隠蔽**: 集約内部のパーツが持つ、自身の状態を変更した新しいインスタンスを返すメソッドは、原則として `internal` 可視性とする。これにより、他モジュールから集約ルートを介さずに内部パーツが直接操作されることを防ぐ。
*   **参照系メソッドの公開**: 状態を変更しない参照専用のメソッド（例: `contains`, `any` 等）は、利便性のために `public` としてもよい。

---

### 3. ドメインイベント (Domain Events)


*   **単一のインターフェース**: 集約ごとに基底となるイベントインターフェース（例: `TravelEvent`）を `sealed interface` で定義する。
*   **過去形の命名**: イベントは「システム内で既に発生した確定事実」であるため、命名は必ず過去形（例: `TravelPlanned`, `FlightSegmentDisrupted`）とする。
*   **最小限のデータ保持**: イベントには「何が起きたか」を表現するために必要十分なデータのみを含める。集約の状態そのもの（例: `oldInfo`）や、イベントの発生に直接関係のない冗長な値を含めてはならない。
*   **必須情報**: すべてのイベントは、共通基底（`DomainEvent`）の規約に従い、`id` (イベントID)、`aggregateId` (対象集約のID)、`sequenceNumber` (バージョン管理用)、および `meta` (発生日時などのメタデータ) を保持する。

---

### 4. エラー設計 (Error Design)

*   **階層構造**: ドメインごとに `sealed interface` を用いてエラーの階層を構築する（例: `TravelError`, `FlightInfoError`）。
*   **バリデーション vs ビジネスルール**:
    *   **不変条件違反**: 複数の入力項目を同時にチェックし、不変条件を満たさないエラーをすべて収集すべき場合は `Either<NonEmptyList<InvariantError>, Success>` を返す (`mapOrAccumulate` を利用)。
    *   **ビジネスルールエラー**: ドメインの前提条件を満たさず、即座に処理を中断すべきエラー。単一のエラーを返す。

---

### 5. ドメイン層のロジック実行 (Command & Factory/Handler)

#### 5.1 Command の定義
*   **Command オブジェクト**: ユースケースからの要求（入力パラメータ）をドメイン層で安全に取り扱うため、`sealed interface` を基底とした Command クラス（例: `TravelCommand`）を定義する。
*   ロジックの実行主体は、この Command を受け取り、ビジネスロジックを実行して新しい状態（State）と発生したイベント（Event）を決定する純粋関数として実装する。

#### 5.2 命名規則と責務の分離
「Service」という曖昧な名称は避け、そのロジックがドメインにおいて果たす**「役割」を明示した名詞**を名称に使用する。また、扱うコマンドや役割ごとにクラスや `object` を明確に分離する。

*   **新規作成（Factory）**:
    新たな集約を生成する責務。複数の Value Object を組み合わせるなど、複雑な初期化ロジックを受け持つ。
    標準シグネチャ: `(Command, Dependencies...) -> Either<Error, Pair<State, Event>>`
    例: `TravelFactory`, `FlightInfoFactory`

*   **既存集約の更新（Role-based Logic / Behavior）**:
    既存の状態を変更する責務。集約クラスから分離されたビジネスロジックを実装する。
    - **Objectとしての実装**: 発見性と責務の明確化のため、ドメインサービスは `object` として定義し、名称はファイル名と一致させる（例: `TravelLifecycleService`）。
    - **戻り値の強制**: サービスメソッドは、状態変更後の集約と発生したイベントの整合性を担保するため、必ず `Either<Error, Pair<State, Event>>` を戻り値の型とする。
    標準シグネチャ: `(State, Command/Parameters, Dependencies...) -> Either<Error, Pair<State, Event>>`
    例:
    - `TravelLifecycleService.start(travel, occurredAt)`
    - `FlightStatusReflector.execute(state, command, api)`

#### 5.3 外部依存の注入（高階関数による部分適用）
集約自身で完結できないロジック（外部システムへの問い合わせや他集約の参照）が必要な場合、重いインターフェースではなく**関数型（ラムダ）として依存を注入**する。

必要な依存（関数）を引数として受け取り、最終的にユースケースが扱いやすい純粋関数（`(Command) -> Result` または `(Command, State) -> Result`）を返す高階関数を定義する。これにより、ロジックの純粋性を保ちつつ、テスト時のモック化を容易にする。

```kotlin
// 例: 依存を受け取り、実行可能な関数（クロージャ）を返す実装
object OrderItemAdder {
    fun makeHandler(
        checkStock: (Product, Int) -> Boolean // 外部依存を関数として注入
    ): (AddItemCommand, OrderState) -> Either<Error, Pair<OrderState, OrderEvent>> {
        return { command, state -> 
            // 注入された関数を使った純粋なロジック
        }
    }
}
```
