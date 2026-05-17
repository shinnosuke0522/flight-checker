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
 * 不変条件違反を表すエラーの基底インターフェース。
 *
 * 「その値または組み合わせがドメイン上成立していない」ことを意味する。
 *
 * ## 判断基準
 * - 外部状態（DB・他エンティティ・現在時刻）に依存しないか？ → YES
 *
 * ## 例
 * - メールアドレスの形式が不正
 * - 文字数制限違反
 * - 数値が範囲外
 * - 開始日が終了日より後になっている
 *
 * ## 境界
 * - 複数の値の関係性に依存する場合は [CompositeInvariantError] を使用する
 * - 外部状態に依存する場合は [BusinessRuleError] を使用する
 */
interface InvariantError : DomainError

/**
 * プリミティブな値に対する不変条件違反エラー。
 *
 * @property valueName 検証対象の名前（例: "email", "age"）
 * @property value 実際に入力された値（nullの場合は未入力などを意味する）
 */
interface PrimitiveInvariantError : InvariantError {
    val valueName: String
    val value: String?
}

/**
 * コレクションに関する不変条件違反エラー。
 */
interface CollectionInvariantError : InvariantError {
    val collectionName: String
}

/**
 * 複数の値の組み合わせや関係性に対する不変条件違反エラー。
 *
 * 「各値単体は正しいが、組み合わせとして不変条件を満たしていない」ことを表す。
 *
 * ## 判断基準
 * - 単一の値ではなく、複数の値を同時に見る必要があるか？ → YES
 * - 外部状態には依存しないか？ → YES
 *
 * ## 例
 * - 開始日が終了日より後になっている
 * - パスワードと確認用パスワードが一致しない
 */
interface CompositeInvariantError : InvariantError

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
 * - 値単体の不正は [InvariantError]
 * - 値の組み合わせのみで判定できる場合は [CompositeInvariantError]
 */
interface BusinessRuleError : DomainError
