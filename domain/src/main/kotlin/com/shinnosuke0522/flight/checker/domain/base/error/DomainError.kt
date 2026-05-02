package com.shinnosuke0522.flight.checker.domain.base.error

/**
 * ドメインロジックに起因するエラーの基底インターフェース。
 *
 * ## 特徴
 * - 想定内の失敗（仕様として定義されている）
 * - ユーザー操作やユースケースの結果として発生する
 * - 基本的に例外として throw せず、戻り値として扱う（Either / Result 等）
 *
 * ## 非対象
 * - プログラムのバグ（NullPointerException など）
 * - インフラ障害（DB接続失敗など）
 */
interface DomainError : Error

/**
 * 入力値または値オブジェクト単体の不正を表すエラー。
 *
 * 「その値自体がドメイン上成立していない」ことを意味する。
 *
 * ## 判断基準
 * - 外部状態（DB・他エンティティ・現在時刻）に依存しないか？ → YES
 *
 * ## 例
 * - メールアドレスの形式が不正
 * - 文字数制限違反
 * - 数値が範囲外
 *
 * ## 境界
 * - 複数の値の関係性に依存する場合は [InvariantViolationError] を使用する
 * - 外部状態に依存する場合は [BusinessRuleError] を使用する
 */
interface ValidationError : DomainError

/**
 * 単一の値に対するバリデーションエラー。
 *
 * @property valueName 検証対象の名前（例: "email", "age"）
 * @property value 実際に入力された値（nullの場合は未入力などを意味する）
 */
interface ValueValidationError : ValidationError {
    val valueName: String
    val value: String?
}

/**
 * 複数の値の組み合わせや関係性に対するバリデーションエラー。
 *
 * 「各値単体は正しいが、組み合わせとして不正である」ことを表す。
 *
 * ## 判断基準
 * - 単一の値ではなく、複数の値を同時に見る必要があるか？ → YES
 * - 外部状態には依存しないか？ → YES
 *
 * ## 例
 * - 開始日が終了日より後になっている
 * - パスワードと確認用パスワードが一致しない
 */
interface InvariantViolationError : ValidationError

/**
 * ドメイン上の業務ルール違反を表すエラー。
 *
 * 「値自体は正しいが、その状態や操作がドメイン的に許可されていない」ことを意味する。
 *
 * ## 判断基準
 * - 単一の値ではなく文脈（コンテキスト）が必要か？ → YES
 * - 外部状態（DB・他エンティティ・現在時刻など）に依存するか？ → YES
 *
 * ## 例
 * - すでに存在するメールアドレスで登録しようとした
 * - 在庫がない商品を購入しようとした
 * - 不正なステータス遷移を行った
 *
 * ## 境界
 * - 値単体の不正は [ValidationError]
 * - 値の組み合わせのみで判定できる場合は [InvariantViolationError]
 */
interface BusinessRuleError : DomainError