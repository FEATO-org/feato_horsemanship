# 専用Skill/Profile修正の検証

2026-10-05、隔離ローカルのPaper 26.2 build 126 / Oracle GraalVM Java 25.0.4 / ValhallaMMO 1.10.3で検証しました。本番データ・World・既存プレイヤーDBは使用していません。

- `clean test` と `clean build`: 17テスト成功。
- 最新インフラGUI設定とのYAML比較: 未使用 `stats` セクションだけを除去し、他の全フィールドを維持。
- 旧Custom Skill YAMLを残した起動: 明確なエラーログで馬術Pluginがdisableし、Listenerを登録しないことを確認。
- 専用Skill/Profile対応・公開APIによるDB初期化後の登録、専用テーブル作成: 成功。
- 43 Perkと6排他UnlockConditionの読み込み、標準reset/refund報酬の登録: 成功。
- 合成UUIDのProfileでSQLite APIによる書込み/読込み、Lv 12・EXP 10.5・累積EXP 4500・NG+ 1の一致: 成功。
- 完全停止・再起動後の同じUUIDのProfile値の一致: 成功。
- PowerProfileの通常/永続PerkリストのDB書込み/読込み: 成功。独自のPerk保存先は追加していません。

DB試験はValhalla公開永続化APIによるもので、プレイヤーの実際のEXP獲得試験ではありません。BetterHorses/DualHorseを含む実プレイヤー操作と本番は未確認です。次の受入試験を完了するまで、全項目完了とは扱いません。

1. `/skills` の馬術表示とツリー、標準 `/valhalla profile HORSEMANSHIP` の表示。
2. 管理者 `/valhalla exp HORSEMANSHIP 10` 前後のEXP/累積EXP差分10。
3. Lv0・First Saddle未取得の操縦者に100ブロック約10 EXP、取得後約10.5 EXP。DualHorse後席に移動EXPが付かないこと。
4. 対象馬上で有効な攻撃に2秒間隔0.5 EXP、EntityDamageByEntityEvent例外がないこと。
5. 実プレイヤーのlogout/login・完全再起動後にLv・EXP・累積EXP・取得Perkが維持されること。
6. 標準reset/refundの実行、Master/Legend NG+、排他取得拒否とruntime safety、他スキル条件・共通ポイント。

移動Listener・戦闘Listener・効果設定のEXP仕様は変更していません。単体の倍率計算は確認しましたが、実際の移動距離や攻撃条件を検証したことにはなりません。
