# FEATO Horsemanship

Paper用のValhallaMMO Custom Skill「馬術 / Horsemanship」です。騎手の成長を担当し、BetterHorsesが管理する馬の遺伝、Trait、Training、base attributeを直接変更しません。標準のActionBarとチャットを使うため、専用クライアントHUDは不要です。

## 対象

- Minecraft / Paper 26.2 build 126、Java 25
- ValhallaMMO 1.10.3 必須
- BetterHorses 6.3 任意。未導入時は騎乗・戦闘の基本機能が動作し、Horsekeeping連携が無効になります。
- DualHorse 1.5.4 任意。専用APIやNMSには依存しません。

対象mountの初期値は `HORSE`、`SKELETON_HORSE`、`ZOMBIE_HORSE`、`DONKEY`、`MULE` です。`eligible-mounts` で変更できます。CamelとLlamaは初期設定で対象外です。

## インストール

1. `src/main/resources/horsemanship.yml` をサーバーの `plugins/ValhallaMMO/skills/custom/horsemanship.yml` にコピーします。既存ファイルは自動上書きしません。
2. `./gradlew clean build` で生成した `build/libs/feato-horsemanship-0.1.0.jar` を `plugins/` に置きます。
3. ValhallaMMOとFEATO Horsemanshipを含めてサーバーを再起動します。Custom SkillはValhallaMMO起動時に登録されます。
4. BetterHorsesの `mounted-damage-boost.enabled: false` を設定してください。馬上戦闘の倍率はFEATO Horsemanshipが担当します。

Valhallaのスキルが未登録の場合、FEATO Horsemanshipは理由をログに出して有効化を中止します。BetterHorsesがない場合はHorsekeepingのみ無効になり、警告を出します。

## EXPと騎乗

- 移動EXPは対象mountの最初のPlayer乗客、つまり操縦者のみが得ます。後席には付与しません。
- 有効距離をUUID単位で蓄積し、初期値100ブロックごとにEXPを一括付与します。ワールド変更、テレポート、対象mount変更、不自然な距離ジャンプは加算しません。
- 馬上戦闘EXPは実際にダメージを与えた本人に少量付与し、初期値2秒の間隔を設けます。DualHorse後席の本人による攻撃も対象です。
- First Saddle取得前の操縦中速度は初期値-5%、取得後-2%、Rein Sense取得後はペナルティなしです。速度は一時AttributeModifierで適用します。

## 追うと分岐

Lv20の「追う」は操縦者がmain handに `LEAD` を持って右クリックすると発動します。初期値は応答1秒、+6%速度、5秒、再使用35秒です。終了後は疲労を先に判定します。疲労の初期値は15%、3秒、速度-10%、最低確率5%。疲労しなかった場合に限り、Lv100の「人馬一体」を50%で判定します。基礎疲労率での実効確率は42.5%です。人馬一体は直前の速度bonusの25%を4秒維持し、防御bonusはありません。

- Mobility: Fleetfoot、Breakaway、Full Gallop、Quick Response、Lightning Start、Ride the Wind、Windborne
- Endurance: Long Haul、Relentless Pace、Trailwise、Steady Pace、Second Wind、Iron Journey、Endless Road
- Technical: Steady Hands、Over the Fence、Surefooted、Calm Rein、Fine Control、Sure Landing、Master of the Reins
- Combat: Cavalier、Charge!、Mounted Marksman、Veteran Cavalry、Heavy / Light Cavalry、First Impact、Iron Vanguard / Swift Rider
- Horsekeeping: Horse Sense、Breeder's Insight、Bloodline Study、Horse Whisperer

BreakawayとRelentless Pace、HeavyとLight Cavalry、Iron VanguardとSwift Riderは排他です。ValhallaのCustom Skill読み込み前に独自の公開UnlockConditionを登録し、相手側Perkの取得済み状態を取得画面で確認します。既存の矛盾データ・両取得時には両側の効果と後続perk効果を抑止してログへ警告します。refund/reset後はValhallaの現在の取得状態から再判定します。登録が失敗した場合は警告を出し、実行時保護だけが残ります。Over the FenceとCharge!は追うの速度profileから独立して併用できます。Charge!自体に速度加算はありません。

Iron VanguardはHeavy Cavalryに加えて `HEAVY_WEAPONS:60`、Swift RiderはLight Cavalryに加えて `LIGHT_WEAPONS:60` を要求します。外部スキルのOR条件をYAMLで推測表現せず、仕様で許可された単一路線を採用しました。Mounted Marksmanは `ARCHERY:50`、Breeder's Insightは `FARMING:50`、Bloodline Studyは `FARMING:60` です。

Lv100のNG+ Master / LegendはValhalla標準のskill resetと永続取得Perkを使います。Masterで疲労率-2%、応答-0.2秒、EXP+10%、Horsekeeping補助+5%。Legendでさらに疲労率-2%、応答-0.2秒、CD-2秒です。疲労率は5%未満、応答は0.1秒未満になりません。人馬一体の条件付き50%は上昇しません。

## Horsekeeping

BetterHorsesがある場合、`/horsemanship inspect` はHorse Sense取得者に馬の現在base Health / Speed / Jump、Gender、Trait、Training進捗を表示します。Breeder's Insight取得者は `/horsemanship parent` で見ている親をUUIDで記録し、もう一方を見て `/horsemanship predict` で理論上の子stat範囲を確認できます。Bloodline Study取得者には設定順のTrait実効確率、none確率、繁殖CDも表示します。Horse Whisperer取得者には親の比較値を追加表示します。権限で受け取れないTraitが抽選に当たると、BetterHorses 6.3と同様にそこで抽選が終了し、none確率に入れます。

Brushing / Feeding補助は各操作イベントの前後のPDC units差分を使います。BetterHorses自身の獲得が0なら補助も0です。追加はBetterHorsesの `TrainingManager` 経由です。馬への回復補助は、その給餌操作に対応する `EATING` 回復イベントが確認できた場合だけ適用します。Trainingによる最大HP再計算のHP変化は回復として扱いません。同じ馬への同tickの給餌をプレイヤー別に判別できない場合は回復補助を省略します。アイテム消費や繁殖処理は変更しません。Iron Journeyの回復補助は既存の馬回復イベントが発生した場合だけ適用します。子のstat、mutation、Trait抽選結果は一切書き換えません。Trainingが無効なカテゴリは `/horsemanship inspect` で「無効」と表示します。

## 設定と近似

`config.yml` の数値は確定バランスではなく、全て調整可能な保守的初期値です。特に移動EXP、戦闘EXP、distance上限、追うの時間・速度・CD、疲労、巡航の到達時間と速度、各分岐の小効果、Training・回復補助、First Impactの必要速度とknockbackをサーバー側で調整してください。Trailwiseは同じ馬で初期値1000ブロック以上移動すると馬術EXPが初期値+3%になり、追うのCD短縮も維持します。他のEXP補正との合計は初期値1.5倍が上限です。`debug`も設定できます。変更後は `/horsemanship reload` で反映できます。

Paper APIで直接表せない加速立ち上がり、旋回、着地の速度損失、ジャンプ距離は `handling.enabled` で切り替えられる小さなvelocity補正で近似しています。Lightning Startは現在速度が馬のmovement speed未満の間だけ加速します。Mounted Marksmanは飛び出した矢を視線方向へわずかに補正し、存在しないValhallaの「馬上射撃ペナルティ」は解除しません。独自staminaはありません。

## ビルドと検証

```sh
./gradlew clean build
```

### GitHub Actionsで手動ビルド・リリース

GitHubの **Actions → Manual build and release → Run workflow** で`version`（例: `0.2.0`）を入力できます。`publish_release` をオフにすると、指定バージョンのJARをJava 25でテスト・ビルドし、JAR、`horsemanship.yml`、`SHA256SUMS` を実行結果のArtifactに保存します。オンにすると、デフォルトブランチのコミットに`v0.2.0`形式の注釈付きタグを自動作成し、同じ3ファイルを添付したGitHub Releaseを公開します。

入力した`version`はJAR名、Gradleのproject version、プラグイン内の`plugin.yml`にも使われます。既存タグが別コミットを指す場合は失敗し、同じコミットを指すタグならRelease作成を再試行できます。`-rc1`などの接尾辞付きバージョンはpre-releaseとして公開します。ローカルビルドでは`build.gradle`の既定値`0.1.0`を使用し、`./gradlew -PbuildVersion=0.2.0 clean build`で上書きできます。GitHub側でActionsの実行とRepository contentsへの書き込みが許可されている必要があります。

通常Perkの報酬はValhalla標準の永続取得リストへ記録し、馬術効果も同じ取得リストを読みます。NG+効果は永続取得したMaster / Legendから判定します。Paper APIは `compileOnly`、ValhallaMMOとBetterHorsesはサーバー側JARを使用し、shadeしません。ValhallaとBetterHorsesの公開メソッドへのアクセスは各integration adapter内に限定しています。pure logicとCustom Skill YAMLのJUnitテストを含みます。Paper 26.2 build 126 / Java 25 / ValhallaMMO 1.10.3 / BetterHorses 6.4によるローカル起動・正常停止を確認しました。プレイヤーによるPerk取得や馬上動作は別途検証が必要です。
