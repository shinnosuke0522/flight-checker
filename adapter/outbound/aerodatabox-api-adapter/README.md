# AeroDataBox API Adapter

このモジュールは、フライトや空港、航空機などの航空関連データを提供する外部API **AeroDataBox API** と通信するためのAdapter（Outbound）です。

## APIの出どころ・仕様

* **API提供元**: [AeroDataBox (RapidAPI)](https://rapidapi.com/aedbx-aedbx/api/aerodatabox)
* **ドキュメント**: 上記のRapidAPIのページ、または [公式API Playground](https://doc.aerodatabox.com/) を参照してください。
* **OpenAPI定義**: 本Adapterは、公式提供されているOpenAPIの定義（JSON/YAML）を元にリクエスト/レスポンスのモデルやクライアントの処理を構成・自動生成することを前提としています。

## 主な機能（APIが提供するデータ）

AeroDataBox APIを利用することで、主に以下の情報が取得可能です。
（※プランやエンドポイントにより取得可能なデータは異なります）
* フライトの出発・到着スケジュールやステータス（リアルタイム）
* 航空機の機体情報や画像
* 世界中の空港情報（IATA/ICAOコード、タイムゾーン、位置情報など）
* フライトの遅延統計情報

## 認証・利用方法

このAPIはRapidAPI経由で提供されているため、リクエスト時に以下のHTTPヘッダーを付与する必要があります。

* `X-RapidAPI-Host`: `aerodatabox.p.rapidapi.com`
* `X-RapidAPI-Key`: (ご自身のRapidAPIアカウントで取得したAPIキー)

APIキーは機密情報のため、ソースコードに直接ハードコードせず、環境変数やSpringの `application.yml`（シークレット管理）から読み込んで利用するようにしてください。
