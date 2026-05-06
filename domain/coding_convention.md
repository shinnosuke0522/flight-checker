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
*   **生成時のバリデーション**: ドメインオブジェクト（特に Value Object）は、生成時点で不正な状態を排除する。不正な状態のオブジェクトはシステム内に存在させてはならない。
*   **Smart Constructor**: `Companion.invoke` や専用の `create` メソッドを隠蔽されたコンストラクタ（`private constructor`）と組み合わせて使用し、バリデーションを経由しないインスタンス化を防ぐ。

#### 1.4 ExceptionではなくErrorベースのエラーハンドリング
*   **例外スローの禁止**: ドメインルールの違反や入力の不正に対しては、絶対に `throw Exception` を行わない。
*   **Eitherによる明示**: 失敗する可能性のある処理は、必ず戻り値の型として `Either<Error, Success>` を返す。これにより、エラーの可能性を型システムで強制的に処理させる。

---

### 2. Aggregate Root (集約ルート)

#### 2.1 ES + FP + OOP の思想に則った apply と replay の実装
*   **イベントソーシング (ES)**: 状態の変更は、直接プロパティを書き換えるのではなく、過去の事実である「イベント」を蓄積し、適用することで表現する。
*   **オブジェクト指向 (OOP) によるカプセル化**: 状態の適用ルールは集約内部にカプセル化する。`EventSourcingAggregateRoot` を継承し、`protected apply(event: EVENT): SELF` を実装することによって「イベントを受けた新しい自身の状態（不変）」を返す。外部から直接 `apply` を呼び出すことは禁止し、リプレイ（再構築）には `replay` メソッドを提供する。
*   **状態の復元**: 過去のイベントのリスト（`NonEmptyList<EVENT>`）を受け取り、初期状態から順に `apply` を適用して現在の状態を復元する `Companion.replay` メソッドを実装する。

#### 2.2 不変条件の強制 (Factory Methodの徹底)
*   **copyメソッドの禁止**: 集約ルート（Aggregate Root）内では、Kotlin の `copy` メソッドの使用を禁止する。これは、`copy` がバリデーションをバイパスして不正な状態のオブジェクトを生成できてしまうためである。
*   **Factory Method経由の生成**: 状態の変更や生成は必ず `Companion.invoke` または専用のファクトリメソッド経由で行い、不変条件（Invariants）を常にチェックする。
*   **例外**: `DomainPrimitive` や、複雑な不変条件を持たない単純なバリューオブジェクトについては `copy` の使用を許容する。

#### 2.3 オブジェクトメソッドとしてのロジック実装
*   **自己完結性**: 外部（DBや他ドメイン）の情報が不要で、集約内部の状態のみで完結するビジネスロジックは、集約のメソッドとして実装する。
*   **戻り値の構造**: メソッドは処理結果として `Either<Error, Pair<State, Event>>` を返すこと。成功時には、**新しい状態（State）と発生した事実（Event）の両方** を必ず含める。

#### 2.4 サブタイプの活用による状態遷移の表現
*   **型の切り替え**: 集約のライフサイクルにおいて、状態（Status）の遷移によって保持すべきプロパティが大きく変わる場合や、許容されるビジネスロジックが変わる場合は、Enumによる分岐ではなく、Kotlinの `sealed interface` または `sealed class` を用いてサブタイプとして表現する。
    *(例: 未確定のフライト `ScheduledFlightInfo` と、遅延が確定した `DelayedFlightInfo` を型レベルで分けることで、不正な状態遷移をコンパイルエラーとして防ぐ。)*

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
    *   **バリデーションエラー**: 複数の入力項目を同時にチェックし、エラーをすべて収集すべき場合は `Either<NonEmptyList<ValidationError>, Success>` を返す (`mapOrAccumulate` を利用)。
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

*   **既存集約の更新（Role-based Logic）**:
    既存の状態を変更する責務。汎用的な「Handler」という名称に縛られず、ロジックの本質を突いた名称を選択する。
    標準シグネチャ: `(Command, State, Dependencies...) -> Either<Error, Pair<State, Event>>`
    例:
    - `FlightStatusReflector` (外部の動静を反映するもの)
    - `TravelRescheduler` (スケジュールを再構築するもの)
    - `PriceAdjuster` (価格や数値を調整するもの)
    - `PolicyVerifier` (複雑な相関チェックを行うもの)

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
