# 題材重複調査レポート

## 調査対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 難易度プロファイル | 実践・上級寄りの基礎教材 |
| 候補題材 | Spring `@Transactional` とチェック例外のロールバック漏れ |
| 観測可能な契約 | 注文作成中に `OrderRejectedException` が発生した場合、期待値は注文0件だが、バグ状態では注文1件が最終DBに残る |
| 直接原因 | Springのデフォルトロールバック規則ではチェック例外は自動ロールバック対象にならない |
| カタログ更新日時 | Repository Catalogは `/home/ubuntu/repository-catalog` に存在せず、更新・検証は未実施 |
| 検索語 | Java, Spring, transaction, rollback, checked exception, order, JPA, debugging |

## 調査範囲

指定済みGitHubアカウント `tonbiattack` のリポジトリ一覧を取得し、Java／Spring／デバッグ関連の候補を読み取り専用で確認した。具体的には `java-unicode-character-limit-debug-lab`、`spring-jpa-orphan-removal-debug-lab`、`spring-security-cors-preflight-debug-lab`、`spring-jpa-optimistic-lock-debug-lab`、`spring-nested-validation-debug-lab`、`language-agnostic-debugging-lab` のREADMEを比較した。また、Qiita管理リポジトリの `public/` と記事カタログを検索し、Springの `@Transactional` に関する既存記事本文も確認した。

## 近接候補の比較

| 既存リポジトリ／記事 | 既存の原因 | 既存の実境界・最終観測 | 今回の差分 | 判定 |
| --- | --- | --- | --- | --- |
| `spring-jpa-orphan-removal-debug-lab` | JPA関連から子を外しても `orphanRemoval` がない | 親子エンティティの統合テストと子テーブル最終件数 | トランザクション例外規則が原因であり、JPA関連マッピングではない。最終観測は注文行のコミット有無 | 重複なし |
| `spring-jpa-optimistic-lock-debug-lab` | バージョン不一致による楽観ロック競合 | 同時更新後の在庫値と例外 | 競合制御ではなくチェック例外のロールバック規則を扱う | 重複なし |
| `spring-nested-validation-debug-lab` | ネストDTOの `@Valid` 付け忘れ | HTTP 400と注文作成件数 | HTTP入力検証ではなく、サービス終了後のDBトランザクション状態を観測する | 重複なし |
| `spring-security-cors-preflight-debug-lab` | CORSプリフライトへの認証要求 | HTTP OPTIONSのステータスとヘッダー | Webセキュリティ境界ではなく、Springトランザクション境界を扱う | 重複なし |
| `language-agnostic-debugging-lab` | Java CLIの別の言語・標準ライブラリ挙動 | CLIの戻り値・出力 | Spring JDBCとH2の統合テストで最終永続状態を検証する | 重複なし |
| Qiita既存記事「Springの@Transactionalはなぜ便利なのに分かりにくいのか」 | チェック例外、自己呼び出し、プロキシなどを概説 | 実務上の注意点をコード例で説明。単独の再現プロジェクトやDB読み戻しテストはない | 直接原因は共通するため高近接。ただし本ラボは、注文拒否という一つの契約、失敗コミット、実行ログ、H2最終状態、`rollbackFor` の回帰テストに限定する | 要レビュー・差分明記 |

## 結論

**作成済みのラボは維持するが、既存の `@Transactional` 解説記事との関係を記事内で明示し、単なる概説の再掲にしない。** 直接原因は既存記事と共通するため、完全な意味での新規テーマとは言えない。一方で、実境界、観測契約、検証手順、Git上のバグ／修正コミットを追加した実証ラボであり、既存記事の内容を名称だけ変えた再実装ではない。

なお、Repository Catalogが存在しなかったため、全ローカル教材を網羅した新規性判定はできない。この限界を残したまま、リモートの既存リポジトリ一覧とQiita管理リポジトリの公開記事を対象に調査した。

## 作成前チェック

- [ ] Repository Catalogを更新・検証した。カタログ未配置のため未実施。
- [x] GitHubアカウント内の語彙的な近接候補を抽出した。
- [x] 高近接の既存 `@Transactional` 記事本文を確認した。
- [x] 同じ失敗を注文名だけ変えて再実装していないことを、原因・境界・観測契約・最小修正で比較した。
- [x] 失敗テスト、観測、最小修正、回帰テスト、分離コミットを備える実装計画と検証を完了した。
