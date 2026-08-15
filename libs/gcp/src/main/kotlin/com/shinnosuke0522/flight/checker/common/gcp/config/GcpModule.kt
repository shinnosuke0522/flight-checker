package com.shinnosuke0522.flight.checker.common.gcp.config

import org.koin.dsl.module

val gcpModule = module {
    // GcpProps は、Ktor のアプリケーション起動時に application.conf 等から読み込んで登録する想定です。
    // このモジュールでは、GCP プロジェクト全体で共有するクライアントなどがあれば定義します。
}
