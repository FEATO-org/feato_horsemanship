---
category: improvement
scope: feato-horsemanship
change: 馬術の高レベル帯の要求EXPを緩和し、騎乗移動EXPを50ブロックごと5 EXPへ変更しました。馬術EXP獲得時にValhallaMMO標準の経験値表示を出し、設定で抑制できるようにしました。
---

変更理由: Lv50以降の必要EXPの急増を抑え、EXP獲得を確認しやすくするためです。要求曲線の指数分母を7.6から17.0へ変更し、純騎乗でLv100まで約50時間を目標とします。100ブロックあたり10 EXPの効率、距離の余り、最大Lv100、Perkと既存Profileは維持します。

表示は共通のValhallaAdapter経由で、experience.show-gain-feedback（初期値true）をaddEXPのsilentへ反転して渡します。独自ActionBarや蓄積処理は追加せず、SKILL_ACTIONとCreative制限を維持します。
