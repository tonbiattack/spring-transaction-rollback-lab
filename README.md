# Spring Transaction Rollback Lab

チェック例外を投げるSpringのトランザクション処理で、**拒否された注文がDBに残る**不具合を、失敗するテストから調査して修正する小さな教材です。修正前のテストは最終DB状態を読み戻し、期待値 `0` に対して実際値 `1` となって失敗します。

## 学習の進め方

| 段階 | 実施内容 | 確認すること |
| --- | --- | --- |
| 再現 | `OrderServiceTest` を実行する | 例外は正しく発生するのに、注文行が残る |
| 観測 | 例外種別とDBの最終状態を分けて確認する | 中間のINSERTではなく、トランザクション終了後の状態を根拠にする |
| 修正 | `rollbackFor = OrderRejectedException.class` を指定する | チェック例外でもロールバックされる |
| 回帰防止 | 同じテストを再実行する | 注文行が0件になる |

## 必要な環境

| 項目 | バージョン |
| --- | --- |
| JDK | 21以上 |
| Maven | 3.8以上 |
| Spring Boot | 3.4.5 |
| DB | H2インメモリ |

## 修正後のテストを実行する

```bash
mvn --batch-mode test
```

## バグを自分で再現する

```bash
git checkout 52188e4
mvn --batch-mode -Dtest=OrderServiceTest test
# expected: 0, but was: 1

git checkout main
mvn --batch-mode -Dtest=OrderServiceTest test
# BUILD SUCCESS
```

## 何がバグなのか

`createOrderThenReject` は注文をINSERTした後に、業務上の拒否を表すチェック例外 `OrderRejectedException` を投げます。しかしSpringのデフォルト設定では、トランザクションは通常 `RuntimeException` と `Error` をロールバック対象とし、チェック例外では自動ロールバックされません。そのため、呼び出し元からは例外が見えている一方で、INSERTがコミットされます。

修正では、業務例外を広くRuntimeExceptionへ変更したり、テストを弱めたりせず、対象の例外型だけを `rollbackFor` に指定しました。このラボでの契約は「注文拒否時は注文行を残さない」であり、最終状態を検証する同じテストが回帰テストになります。

## プロジェクト構成

```text
src/main/java/com/example/rollbacklab/
├── RollbackLabApplication.java
├── OrderRejectedException.java
└── OrderService.java
src/test/java/com/example/rollbacklab/
└── OrderServiceTest.java
docs/
└── debugging-record.md
.github/workflows/maven.yml
```

詳細な観測と仮説の切り分けは [デバッグ記録](docs/debugging-record.md) に記載しています。

## References

[1] [Spring Framework Reference: Rolling Back a Declarative Transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)

[2] [Spring Framework Reference: Using `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
