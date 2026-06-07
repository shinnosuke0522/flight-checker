package com.shinnosuke0522.flight.checker.domain.ticket.policy

import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId

/**
 * チケットの重複登録に関する判断基準（ポリシー）。
 *
 * 外部の事実（既存の登録状況）に基づき、登録の可否を判断するビジネスルールを表現する。
 */
fun interface TicketDuplicatePolicy {
    /**
     * 指定されたユーザーとフライトの組み合わせが、既に登録済み（重複）であるか判定する。
     *
     * @return 重複している場合は true、そうでない場合は false
     */
    fun isDuplicate(userId: UserId, flightIdentity: FlightIdentity): Boolean
}
