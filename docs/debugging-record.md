# デバッグ記録: チェック例外で注文がロールバックされない

## 目的

注文作成中に業務上の拒否が発生した場合、呼び出し元には `OrderRejectedException` を返し、トランザクション終了後の `orders` テーブルには注文行を残さない。このラボでは、例外の発生だけでなく、DBの最終状態を独立に確認する。

## 最初に観測した事実

バグ状態のコミットは `52188e4` です。`mvn --batch-mode -Dtest=OrderServiceTest test` を実行すると、例外のアサーションは成功するが、最終状態のアサーションが失敗した。

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| サービス呼び出し | `OrderRejectedException` が発生 | `OrderRejectedException` が発生 | `assertThatThrownBy` |
| INSERT | 注文作成処理を開始する | `order-1` を `CREATED` でINSERT | `OrderService#createOrderThenReject` |
| 最終状態 | `orders` の `order-1` は0件 | 1件残る | テスト後のSELECT |

```text
[拒否された注文はトランザクション終了後に残らない]
expected: 0
 but was: 1
```

ここで、INSERTログだけをコミットの証拠とは扱わず、サービス呼び出しが完了して例外を送出した後に別のJDBCクエリで読み戻した結果を採用した。

## テストの境界

Springのトランザクション境界とH2への永続化結果が問題なので、単体テストではなく `@SpringBootTest` とインメモリH2を使う統合テストにした。モック呼び出しの有無では、トランザクションがコミットされたかロールバックされたかを確認できないためである。テストは例外と最終DB状態を分けてアサートし、失敗がセットアップやコンパイルではなく契約差分であることを示す。

## 仮説と切り分け

| 仮説 | 確認方法 | 結果 |
| --- | --- | --- |
| INSERTが実行されていない | 例外送出直後の処理と、最終SELECTの対象IDを確認する | 否定。行はINSERTされ、最終的に1件読める |
| H2の初期化またはテスト順序が原因 | テストは1件だけ実行し、各テスト前にテーブルを再作成する | 否定。単独実行でも同じ差分になる |
| チェック例外がデフォルトのロールバック規則に該当しない | `@Transactional` のロールバック仕様を公式資料と照合する | 採用。チェック例外はデフォルトではロールバックされない |

## 原因

`@Transactional` は存在するため、トランザクション管理自体は有効である。しかし `OrderRejectedException` は `Exception` のサブクラスであり `RuntimeException` ではない。Spring Frameworkのデフォルト規則では、未処理のチェック例外ではロールバックされず、メソッド終了時にINSERTがコミットされる。公式資料は、チェック例外をロールバック対象にするには `rollbackFor` などのロールバック規則を指定できると説明している。[1]

## 修正

```java
@Transactional(rollbackFor = OrderRejectedException.class)
public void createOrderThenReject(String orderId) throws OrderRejectedException {
    jdbcTemplate.update("insert into orders (id, status) values (?, ?)", orderId, "CREATED");
    throw new OrderRejectedException("在庫確認に失敗したため注文を拒否しました");
}
```

対象の業務例外だけを明示的にロールバック対象へ追加する最小修正である。例外をRuntimeExceptionへ変更する方法は、公開APIの例外契約を変えるため採用しなかった。また、テストで行を削除する方法は本番挙動を修正せず、テストを弱めるため不適切である。

## 再発防止テスト

修正前後で同一の `rejectedOrderIsNotPersisted` を実行する。まず例外型を確認し、その後にサービス呼び出しの外側からSELECTして、トランザクション終了後の最終状態を検証する。修正前は `expected: 0 but was: 1` で失敗し、修正後は成功する。

## 再現手順

```bash
git checkout 52188e4
mvn --batch-mode -Dtest=OrderServiceTest test

# 修正済みのデフォルトブランチへ戻す
git checkout main
mvn --batch-mode -Dtest=OrderServiceTest test
```

## 適用範囲と注意点

この修正は、チェック例外を業務上の失敗として扱い、その失敗時に同一トランザクションの書き込みを取り消したいケースに有効である。`@Transactional` は通常プロキシ経由の外部呼び出しで適用されるため、同一クラス内の自己呼び出しなど、別のトランザクション適用問題にはこの修正だけでは対処できない。[2] 実際のシステムでは、例外設計、複数データソース、非同期処理、外部API呼び出しとの整合性も別途検討する必要がある。

## References

[1] [Spring Framework Reference: Rolling Back a Declarative Transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)

[2] [Spring Framework Reference: Using `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
