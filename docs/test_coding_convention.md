# Testing Conventions

FP+ES (関数型プログラミング + イベントソーシング) アーキテクチャにおける、ドメイン層のテスト戦略とコーディング規約です。
ドメインの関心事である「ビジネスルールの判断」と「状態の適用」を明確に分離し、それぞれに適したブラックボックステストを実施します。

## 共通ポリシー

### テストの構成とスタイル (Kotest DescribeSpec)

ドメイン層のテストは、Kotest の `DescribeSpec` を使用し、**Given-When-Then** 形式で構成します。これにより、ビジネスドキュメントに近い可読性を維持します。

#### 基本構造
*   **`describe`**: テスト対象（クラスや大きな振る舞いの括り）、またはビジネス上の大きなシナリオ（Given）を記述します。
*   **`context("Given: ...")`**: 前提条件（初期状態、依存関係の設定など）をビジネス言語で記述します。
*   **`context("When: ...")`**: 実行する操作（メソッド呼び出し、コマンドの実行）を記述します。
*   **`it("Then: ...")`**: 期待される結果（アサーション）を記述します。

#### 記述に関する重要なガイドライン (ブラックボックス・テストの徹底)
テストの記述（`describe`, `context`, `it`）には、**ビジネス上の関心事**のみを記述し、技術的な実装詳細を漏洩させてはなりません。

*   **ユビキタス言語の使用**: 常にビジネス上の言葉（日本語を推奨）を使用し、「〜の場合」「〜したとき」といった具体的なシナリオを記述します。
*   **技術用語の排除**: `Policy`, `Stub`, `Mock`, `Boolean` といったプログラミング・技術用語を記述に含めてはなりません。
*   **実装詳細の隠蔽**: テストのセットアップで特定の Policy クラスを使用する場合でも、そのクラス名や具体的な Boolean 値（true/false）を記述に含めず、「登録済みのチケットがある場合」「重複がない場合」といったビジネスコンテキストとして表現します。

```kotlin
// ❌ 悪い例（アンチパターン）: 技術的な実装詳細が記述に漏れている
describe("Given: TicketDuplicatePolicy that returns true") {
    context("When: result is Either.Left") {
        it("Then: should return TicketAlreadyRegisteredError") { ... }
    }
}

// ✅ 良い例（ベストプラクティス）: ビジネスシナリオとして記述されている
describe("Given: すでに同じユーザー・フライトで登録済みのチケットがある場合") {
    context("When: チケット登録を実行すると") {
        it("Then: 登録済みエラー (TicketAlreadyRegisteredError) が返却されること") { ... }
    }
}
```

---

## ドメイン層のテスト

### テスト実装の共通ベストプラクティス

#### テスト用データ (Given) はイベントから構築する
状態を持つオブジェクト（集約）の事前状態を作る際は、リフレクションや巨大なコンストラクタを使用せず、**過去のイベントを `replay` して状態を復元する** アプローチを推奨します。これにより、テストコード自体もブラックボックス性を維持できます。

```kotlin
// ヘルパー関数の例
fun givenTravel(vararg events: TravelEvent): Travel {
    return Travel.replay(nonEmptyListOf(*events))
}

// テストでの利用例
context("Given: すでに開始されている旅行がある場合") {
    val travel = givenTravel(
        TravelPlanned(...),
        TravelStarted(...)
    )
    // When: ...
}
```

#### 副作用のない純粋関数のテスト
ドメイン層（ModelおよびService）は純粋関数として実装されているため、原則としてモック（Mocking Framework）は使用しません。外部依存がある場合は、高階関数を用いた依存注入の仕組みを利用し、テスト用のダミー関数（ラムダ）を注入してテストを行います。


### テストの責務の分離

集約（Aggregate Root）とドメインサービスの役割分担に基づき、テストファイルとその目的を明確に分割します。

| テスト対象 (SUT) | テストの目的 | ファイル名の例 | ブラックボックス的視点 |
| :--- | :--- | :--- | :--- |
| **Model (集約クラス)** | 状態遷移 (State Transition) と 生成時バリデーションの検証 | `[AggregateName]Test.kt`<br>例: `TravelTest.kt` | `イベント` または `初期パラメータ` を与えたら、正しい **`状態`** または **`エラー`** になるか |
| **Service (ドメインサービス)** | ビジネスルールの判断 (Decision Making) の検証 | `[ServiceName]Test.kt`<br>例: `TravelLifecycleServiceTest.kt` | `状態` と `コマンド` を与えたら、正しい **`イベント`** または **`エラー`** が出力されるか |

---

### Model のテスト規約 (`xxxTest.kt`)

Model（集約）は純粋関数として振る舞うため、テストは以下の2点にフォーカスし、ビジネスルールの検証は行いません。

#### Factory (生成・不変条件) のテスト
「ありえない初期状態」が作られないことを証明します。

*   **入力:** 初期パラメータ
*   **出力:** 正常な状態（または初期イベント）、またはドメインエラー
*   **検証内容:**
    *   正しいパラメータを与えた場合、エラーにならず想定通りのオブジェクトが生成されること。
    *   不正なパラメータを与えた場合、`Either.Left` として期待するエラーが返却されること。

#### 状態の適用 (State Transition / `apply`, `replay`) のテスト
発生した事実（Event）を受けて、集約の状態がどう遷移するか（どうProjectionされるか）を証明します。

*   **入力:** 現在の状態（State）＋ 発生した事実（Event） または イベントの履歴 (`NonEmptyList<Event>`)
*   **出力:** 新しい状態（State）
*   **検証内容:**
    *   イベントを適用（`apply` または `replay`）した結果、公開プロパティが意図した状態（ステータス、バージョン、保持する値など）に正しく更新されること。
*   **アンチパターン:**
    *   ここでは「そのイベントを発行してよいか」というビジネスルールの検証は行わない（イベントが適用可能である前提でテストする）。

### 3. Service / Command のテスト規約 (`xxxServiceTest.kt`)

Domain Service は副作用を持たない**純粋関数 (Pure Function)** として実装します。そのため、テストは状態の事前設定（Given）と入力値に対する「戻り値（イベントまたはエラー）」の検証に特化します。

#### 振る舞い（ビジネスルール）のテスト
ブラックボックス・テストの観点から、内部ステータスの変化よりも**「どのような事実（Event）が生み出されたか」**または**「どのように拒絶されたか（Error）」**を最優先でアサートします。

*   **入力:** 現在の状態（State）＋ 要求（Command/Parameter）
*   **出力:** 発行されるイベント (`Event`)、またはドメインエラー (`Error`)
*   **検証内容:**
    *   **成功ケース:** 期待する型、内容の **イベントが発行されること** を第一に検証する。副次的に、状態が更新されていることも確認する。
    *   **失敗ケース (ドメインルール違反):** 期待する **エラーが返却され、イベントが発行されないこと** を検証する。
*   **構造の簡素化**: 
    Domain Service のように単一の公開メソッド（入り口）のみを持つ場合、クラス名による冗長なネストを避けるため、トップレベルの `describe` を直接 `describe("Given: ...")` としてシナリオの記述に使用することを推奨する。

```kotlin
// 例: Serviceのテスト（振る舞いのブラックボックス検証）
class TravelLifecycleServiceTest : DescribeSpec({
    describe("Given: 計画済み (PLANNED) の旅行がある場合") {
        val travel = givenTravel(travelPlannedEvent)

        context("When: 旅行を開始(start)すると") {
            val result = TravelLifecycleService.start(travel, now)

            it("Then: 成功し、開始イベント(TravelStarted)が発行されること") {
                val (updatedTravel, event) = result.shouldBeRight()
                event.shouldBeTypeOf<TravelStarted>()
            }
        }
    }
})
```
