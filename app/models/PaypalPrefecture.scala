package models;

import scala.collection.immutable

object PaypalPrefecture {
  val map = immutable.Map[Prefecture, String](
    JapanPrefecture.北海道 -> "北海道",
    JapanPrefecture.青森県 -> "青森",
    JapanPrefecture.岩手県 -> "岩手",
    JapanPrefecture.宮城県 -> "宮城",
    JapanPrefecture.秋田県 -> "秋田",
    JapanPrefecture.山形県 -> "山形",
    JapanPrefecture.福島県 -> "福島",
    JapanPrefecture.茨城県 -> "茨城",
    JapanPrefecture.栃木県 -> "栃木",
    JapanPrefecture.群馬県 -> "群馬",
    JapanPrefecture.埼玉県 -> "埼玉",
    JapanPrefecture.千葉県 -> "千葉",
    JapanPrefecture.東京都 -> "東京",
    JapanPrefecture.神奈川県 -> "神奈川",
    JapanPrefecture.新潟県 -> "新潟",
    JapanPrefecture.富山県 -> "富山",
    JapanPrefecture.石川県 -> "石川",
    JapanPrefecture.福井県 -> "福井",
    JapanPrefecture.山梨県 -> "山梨",
    JapanPrefecture.長野県 -> "長野",
    JapanPrefecture.岐阜県 -> "岐阜",
    JapanPrefecture.静岡県 -> "静岡",
    JapanPrefecture.愛知県 -> "愛知",
    JapanPrefecture.三重県 -> "三重",
    JapanPrefecture.滋賀県 -> "滋賀",
    JapanPrefecture.京都府 -> "京都",
    JapanPrefecture.大阪府 -> "大阪",
    JapanPrefecture.兵庫県 -> "兵庫",
    JapanPrefecture.奈良県 -> "奈良",
    JapanPrefecture.和歌山県 -> "和歌山",
    JapanPrefecture.鳥取県 -> "鳥取",
    JapanPrefecture.島根県 -> "島根",
    JapanPrefecture.岡山県 -> "岡山",
    JapanPrefecture.広島県 -> "広島",
    JapanPrefecture.山口県 -> "山口",
    JapanPrefecture.徳島県 -> "徳島",
    JapanPrefecture.香川県 -> "香川",
    JapanPrefecture.愛媛県 -> "愛媛",
    JapanPrefecture.高知県 -> "高知",
    JapanPrefecture.福岡県 -> "福岡",
    JapanPrefecture.佐賀県 -> "佐賀",
    JapanPrefecture.長崎県 -> "長崎",
    JapanPrefecture.熊本県 -> "熊本",
    JapanPrefecture.大分県 -> "大分",
    JapanPrefecture.宮崎県 -> "宮崎",
    JapanPrefecture.鹿児島県 -> "鹿児島",
    JapanPrefecture.沖縄県 -> "沖縄"
  )

  def apply(pref: Prefecture): Option[String] = map.get(pref)
}
