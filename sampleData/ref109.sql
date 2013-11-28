
-- Shipping box
--insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(1, 1, 1, 15, '宿根草');
--insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(2, 1, 2,  8, 'ポット樹木（小）');
--insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(3, 1, 3,  2, 'ポット樹木（大）');
--insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(4, 2, 1, 50, 'グランドカバー宿根草等専用');
--insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(5, 2, 2, 20, '低木H0.5m以下専用');
--insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(6, 2, 3,  1, '低木H0.6m～H1.5m以下専用');
--insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(7, 2, 4,  1, 'その他規格外');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(8, 2, 5,  1, 'セット送料A');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(9, 2, 6,  1, 'セット送料B');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(10, 2, 7,  1, 'セット送料C');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(11, 2, 8,  1, 'セット送料D');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(12, 2, 9,  1, 'セット送料E');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(13, 2, 10,  1, 'セット送料F');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(14, 2, 11,  1, 'セット送料G');
insert into shipping_box (shipping_box_id, site_id, item_class, box_size, box_name) values(15, 2, 12,  1, 'セット送料H');

-- Shipping fee
-- 1 ~ 325 initial data
-- 北海道
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(326, 8, 152, 1);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(327, 9, 152, 1);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(328, 10, 152, 1);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(329, 11, 152, 1);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(330, 12, 152, 1);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(331, 13, 152, 1);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(332, 14, 152, 1);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(333, 15, 152, 1);

-- 青森県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(334, 8, 152, 2);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(335, 9, 152, 2);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(336, 10, 152, 2);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(337, 11, 152, 2);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(338, 12, 152, 2);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(339, 13, 152, 2);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(340, 14, 152, 2);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(341, 15, 152, 2);

-- 岩手県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(342, 8, 152, 3);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(343, 9, 152, 3);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(344, 10, 152, 3);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(345, 11, 152, 3);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(346, 12, 152, 3);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(347, 13, 152, 3);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(348, 14, 152, 3);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(349, 15, 152, 3);

-- 宮城県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(350, 8, 152, 4);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(351, 9, 152, 4);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(352, 10, 152, 4);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(353, 11, 152, 4);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(354, 12, 152, 4);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(355, 13, 152, 4);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(356, 14, 152, 4);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(357, 15, 152, 4);

-- 秋田県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(358, 8, 152, 5);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(359, 9, 152, 5);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(360, 10, 152, 5);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(361, 11, 152, 5);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(362, 12, 152, 5);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(363, 13, 152, 5);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(364, 14, 152, 5);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(365, 15, 152, 5);

-- 山形県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(366, 8, 152, 6);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(367, 9, 152, 6);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(368, 10, 152, 6);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(369, 11, 152, 6);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(370, 12, 152, 6);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(371, 13, 152, 6);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(372, 14, 152, 6);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(373, 15, 152, 6);

-- 福島県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(374, 8, 152, 7);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(375, 9, 152, 7);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(376, 10, 152, 7);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(377, 11, 152, 7);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(378, 12, 152, 7);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(379, 13, 152, 7);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(380, 14, 152, 7);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(381, 15, 152, 7);

-- 茨城県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(382, 8, 152, 8);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(383, 9, 152, 8);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(384, 10, 152, 8);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(385, 11, 152, 8);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(386, 12, 152, 8);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(387, 13, 152, 8);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(388, 14, 152, 8);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(389, 15, 152, 8);

-- 栃木県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(390, 8, 152, 9);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(391, 9, 152, 9);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(392, 10, 152, 9);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(393, 11, 152, 9);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(394, 12, 152, 9);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(395, 13, 152, 9);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(396, 14, 152, 9);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(397, 15, 152, 9);

-- 群馬県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(398, 8, 152, 10);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(399, 9, 152, 10);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(400, 10, 152, 10);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(401, 11, 152, 10);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(402, 12, 152, 10);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(403, 13, 152, 10);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(404, 14, 152, 10);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(405, 15, 152, 10);

-- 埼玉県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(406, 8, 152, 11);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(407, 9, 152, 11);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(408, 10, 152, 11);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(409, 11, 152, 11);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(410, 12, 152, 11);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(411, 13, 152, 11);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(412, 14, 152, 11);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(413, 15, 152, 11);

-- 千葉県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(414, 8, 152, 12);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(415, 9, 152, 12);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(416, 10, 152, 12);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(417, 11, 152, 12);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(418, 12, 152, 12);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(419, 13, 152, 12);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(420, 14, 152, 12);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(421, 15, 152, 12);

-- 東京都
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(422, 8, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(423, 9, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(424, 10, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(425, 11, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(426, 12, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(427, 13, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(428, 14, 152, 13);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(429, 15, 152, 13);

-- 神奈川県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(430, 8, 152, 14);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(431, 9, 152, 14);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(432, 10, 152, 14);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(433, 11, 152, 14);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(434, 12, 152, 14);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(435, 13, 152, 14);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(436, 14, 152, 14);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(437, 15, 152, 14);

-- 新潟県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(438, 8, 152, 15);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(439, 9, 152, 15);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(440, 10, 152, 15);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(441, 11, 152, 15);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(442, 12, 152, 15);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(443, 13, 152, 15);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(444, 14, 152, 15);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(445, 15, 152, 15);

-- 富山県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(446, 8, 152, 16);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(447, 9, 152, 16);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(448, 10, 152, 16);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(449, 11, 152, 16);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(450, 12, 152, 16);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(451, 13, 152, 16);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(452, 14, 152, 16);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(453, 15, 152, 16);

-- 石川県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(454, 8, 152, 17);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(455, 9, 152, 17);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(456, 10, 152, 17);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(457, 11, 152, 17);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(458, 12, 152, 17);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(459, 13, 152, 17);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(460, 14, 152, 17);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(461, 15, 152, 17);

-- 福井県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(462, 8, 152, 18);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(463, 9, 152, 18);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(464, 10, 152, 18);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(465, 11, 152, 18);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(466, 12, 152, 18);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(467, 13, 152, 18);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(468, 14, 152, 18);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(469, 15, 152, 18);

-- 山梨県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(470, 8, 152, 19);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(471, 9, 152, 19);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(472, 10, 152, 19);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(473, 11, 152, 19);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(474, 12, 152, 19);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(475, 13, 152, 19);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(476, 14, 152, 19);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(477, 15, 152, 19);

-- 長野県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(478, 8, 152, 20);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(479, 9, 152, 20);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(480, 10, 152, 20);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(481, 11, 152, 20);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(482, 12, 152, 20);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(483, 13, 152, 20);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(484, 14, 152, 20);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(485, 15, 152, 20);

-- 岐阜県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(486, 8, 152, 21);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(487, 9, 152, 21);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(488, 10, 152, 21);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(489, 11, 152, 21);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(490, 12, 152, 21);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(491, 13, 152, 21);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(492, 14, 152, 21);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(493, 15, 152, 21);

-- 静岡県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(494, 8, 152, 22);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(495, 9, 152, 22);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(496, 10, 152, 22);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(497, 11, 152, 22);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(498, 12, 152, 22);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(499, 13, 152, 22);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(500, 14, 152, 22);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(501, 15, 152, 22);

-- 愛知県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(502, 8, 152, 23);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(503, 9, 152, 23);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(504, 10, 152, 23);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(505, 11, 152, 23);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(506, 12, 152, 23);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(507, 13, 152, 23);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(508, 14, 152, 23);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(509, 15, 152, 23);

-- 三重県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(510, 8, 152, 24);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(511, 9, 152, 24);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(512, 10, 152, 24);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(513, 11, 152, 24);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(514, 12, 152, 24);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(515, 13, 152, 24);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(516, 14, 152, 24);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(517, 15, 152, 24);

-- 滋賀県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(518, 8, 152, 25);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(519, 9, 152, 25);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(520, 10, 152, 25);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(521, 11, 152, 25);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(522, 12, 152, 25);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(523, 13, 152, 25);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(524, 14, 152, 25);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(525, 15, 152, 25);

-- 京都府
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(526, 8, 152, 26);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(527, 9, 152, 26);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(528, 10, 152, 26);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(529, 11, 152, 26);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(530, 12, 152, 26);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(531, 13, 152, 26);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(532, 14, 152, 26);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(533, 15, 152, 26);

-- 大阪府
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(534, 8, 152, 27);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(535, 9, 152, 27);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(536, 10, 152, 27);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(537, 11, 152, 27);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(538, 12, 152, 27);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(539, 13, 152, 27);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(540, 14, 152, 27);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(541, 15, 152, 27);

-- 兵庫県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(542, 8, 152, 28);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(543, 9, 152, 28);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(544, 10, 152, 28);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(545, 11, 152, 28);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(546, 12, 152, 28);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(547, 13, 152, 28);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(548, 14, 152, 28);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(549, 15, 152, 28);

-- 奈良県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(550, 8, 152, 29);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(551, 9, 152, 29);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(552, 10, 152, 29);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(553, 11, 152, 29);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(554, 12, 152, 29);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(555, 13, 152, 29);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(556, 14, 152, 29);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(557, 15, 152, 29);

-- 和歌山県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(558, 8, 152, 30);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(559, 9, 152, 30);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(560, 10, 152, 30);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(561, 11, 152, 30);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(562, 12, 152, 30);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(563, 13, 152, 30);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(564, 14, 152, 30);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(565, 15, 152, 30);

-- 鳥取県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(566, 8, 152, 31);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(567, 9, 152, 31);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(568, 10, 152, 31);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(569, 11, 152, 31);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(570, 12, 152, 31);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(571, 13, 152, 31);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(572, 14, 152, 31);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(573, 15, 152, 31);

-- 島根県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(574, 8, 152, 32);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(575, 9, 152, 32);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(576, 10, 152, 32);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(577, 11, 152, 32);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(578, 12, 152, 32);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(579, 13, 152, 32);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(580, 14, 152, 32);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(581, 15, 152, 32);

-- 岡山県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(582, 8, 152, 33);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(583, 9, 152, 33);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(584, 10, 152, 33);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(585, 11, 152, 33);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(586, 12, 152, 33);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(587, 13, 152, 33);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(588, 14, 152, 33);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(589, 15, 152, 33);

-- 広島県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(590, 8, 152, 34);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(591, 9, 152, 34);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(592, 10, 152, 34);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(593, 11, 152, 34);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(594, 12, 152, 34);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(595, 13, 152, 34);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(596, 14, 152, 34);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(597, 15, 152, 34);

-- 山口県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(598, 8, 152, 35);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(599, 9, 152, 35);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(600, 10, 152, 35);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(601, 11, 152, 35);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(602, 12, 152, 35);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(603, 13, 152, 35);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(604, 14, 152, 35);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(605, 15, 152, 35);

-- 徳島県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(606, 8, 152, 36);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(607, 9, 152, 36);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(608, 10, 152, 36);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(609, 11, 152, 36);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(610, 12, 152, 36);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(611, 13, 152, 36);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(612, 14, 152, 36);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(613, 15, 152, 36);

-- 香川県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(614, 8, 152, 37);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(615, 9, 152, 37);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(616, 10, 152, 37);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(617, 11, 152, 37);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(618, 12, 152, 37);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(619, 13, 152, 37);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(620, 14, 152, 37);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(621, 15, 152, 37);

-- 愛媛県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(622, 8, 152, 38);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(623, 9, 152, 38);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(624, 10, 152, 38);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(625, 11, 152, 38);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(626, 12, 152, 38);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(627, 13, 152, 38);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(628, 14, 152, 38);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(629, 15, 152, 38);

-- 高知県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(630, 8, 152, 39);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(631, 9, 152, 39);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(632, 10, 152, 39);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(633, 11, 152, 39);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(634, 12, 152, 39);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(635, 13, 152, 39);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(636, 14, 152, 39);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(637, 15, 152, 39);

-- 福岡県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(638, 8, 152, 40);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(639, 9, 152, 40);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(640, 10, 152, 40);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(641, 11, 152, 40);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(642, 12, 152, 40);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(643, 13, 152, 40);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(644, 14, 152, 40);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(645, 15, 152, 40);

-- 佐賀県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(646, 8, 152, 41);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(647, 9, 152, 41);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(648, 10, 152, 41);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(649, 11, 152, 41);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(650, 12, 152, 41);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(651, 13, 152, 41);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(652, 14, 152, 41);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(653, 15, 152, 41);

-- 長崎県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(654, 8, 152, 42);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(655, 9, 152, 42);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(656, 10, 152, 42);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(657, 11, 152, 42);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(658, 12, 152, 42);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(659, 13, 152, 42);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(660, 14, 152, 42);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(661, 15, 152, 42);

-- 熊本県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(662, 8, 152, 43);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(663, 9, 152, 43);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(664, 10, 152, 43);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(665, 11, 152, 43);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(666, 12, 152, 43);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(667, 13, 152, 43);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(668, 14, 152, 43);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(669, 15, 152, 43);

-- 大分県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(670, 8, 152, 44);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(671, 9, 152, 44);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(672, 10, 152, 44);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(673, 11, 152, 44);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(674, 12, 152, 44);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(675, 13, 152, 44);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(676, 14, 152, 44);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(677, 15, 152, 44);

-- 宮崎県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(678, 8, 152, 45);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(679, 9, 152, 45);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(680, 10, 152, 45);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(681, 11, 152, 45);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(682, 12, 152, 45);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(683, 13, 152, 45);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(684, 14, 152, 45);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(685, 15, 152, 45);

-- 鹿児島県
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(686, 8, 152, 46);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(687, 9, 152, 46);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(688, 10, 152, 46);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(689, 11, 152, 46);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(690, 12, 152, 46);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(691, 13, 152, 46);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(692, 14, 152, 46);
insert into shipping_fee (shipping_fee_id, shipping_box_id, country_code, location_code) values(693, 15, 152, 46);

-- 沖縄県
-- no data

-- Shipping fee history
-- 1 ~ 325 initial data
-- 北海道
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(326, 326, 1, 3500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(327, 327, 1, 7000, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(328, 328, 1, 7230, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(329, 329, 1, 11250, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(330, 330, 1, 3730, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(331, 331, 1, 7460, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(332, 332, 1, 11480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(333, 333, 1, 7750, timestamp '9999-12-31 00:00:00');

-- 青森県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(334, 334, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(335, 335, 1, 6200, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(336, 336, 1, 6230, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(337, 337, 1, 10350, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(338, 338, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(339, 339, 1, 6260, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(340, 340, 1, 10380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(341, 341, 1, 7250, timestamp '9999-12-31 00:00:00');

-- 岩手県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(342, 342, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(343, 343, 1, 6200, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(344, 344, 1, 6230, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(345, 345, 1, 10350, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(346, 346, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(347, 347, 1, 6260, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(348, 348, 1, 10380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(349, 349, 1, 7250, timestamp '9999-12-31 00:00:00');

-- 宮城県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(350, 350, 1, 2500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(351, 351, 1, 5000, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(352, 352, 1, 5230, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(353, 353, 1, 9250, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(354, 354, 1, 2730, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(355, 355, 1, 5460, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(356, 356, 1, 9480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(357, 357, 1, 6750, timestamp '9999-12-31 00:00:00');

-- 秋田県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(358, 358, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(359, 359, 1, 6200, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(360, 360, 1, 6230, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(361, 361, 1, 10350, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(362, 362, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(363, 363, 1, 6260, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(364, 364, 1, 10380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(365, 365, 1, 7250, timestamp '9999-12-31 00:00:00');

-- 山形県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(366, 366, 1, 2500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(367, 367, 1, 5000, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(368, 368, 1, 5230, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(369, 369, 1, 9250, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(370, 370, 1, 2730, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(371, 371, 1, 5460, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(372, 372, 1, 9480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(373, 373, 1, 6750, timestamp '9999-12-31 00:00:00');

-- 福島県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(374, 374, 1, 2500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(375, 375, 1, 5000, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(376, 376, 1, 5230, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(377, 377, 1, 9250, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(378, 378, 1, 2730, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(379, 379, 1, 5460, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(380, 380, 1, 9480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(381, 381, 1, 6750, timestamp '9999-12-31 00:00:00');

-- 茨城県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(382, 382, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(383, 383, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(384, 384, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(385, 385, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(386, 386, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(387, 387, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(388, 388, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(389, 389, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 栃木県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(390, 390, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(391, 391, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(392, 392, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(393, 393, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(394, 394, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(395, 395, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(396, 396, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(397, 397, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 群馬県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(398, 398, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(399, 399, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(400, 400, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(401, 401, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(402, 402, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(403, 403, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(404, 404, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(405, 405, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 埼玉県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(406, 406, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(407, 407, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(408, 408, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(409, 409, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(410, 410, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(411, 411, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(412, 412, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(413, 413, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 千葉県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(414, 414, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(415, 415, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(416, 416, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(417, 417, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(418, 418, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(419, 419, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(420, 420, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(421, 421, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 東京都
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(422, 422, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(423, 423, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(424, 424, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(425, 425, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(426, 426, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(427, 427, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(428, 428, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(429, 429, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 神奈川県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(430, 430, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(431, 431, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(432, 432, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(433, 433, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(434, 434, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(435, 435, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(436, 436, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(437, 437, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 新潟県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(438, 438, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(439, 439, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(440, 440, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(441, 441, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(442, 442, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(443, 443, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(444, 444, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(445, 445, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 富山県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(446, 446, 1, 1420, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(447, 447, 1, 2840, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(448, 448, 1, 2900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(449, 449, 1, 6920, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(450, 450, 1, 1480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(451, 451, 1, 2960, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(452, 452, 1, 6980, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(453, 453, 1, 5500, timestamp '9999-12-31 00:00:00');

-- 石川県1
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(454, 454, 1, 1420, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(455, 455, 1, 2840, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(456, 456, 1, 2900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(457, 457, 1, 6920, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(458, 458, 1, 1480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(459, 459, 1, 2960, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(460, 460, 1, 6980, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(461, 461, 1, 5500, timestamp '9999-12-31 00:00:00');

-- 福井県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(462, 462, 1, 1420, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(463, 463, 1, 2840, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(464, 464, 1, 2900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(465, 465, 1, 6920, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(466, 466, 1, 1480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(467, 467, 1, 2960, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(468, 468, 1, 6980, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(469, 469, 1, 5500, timestamp '9999-12-31 00:00:00');

-- 山梨県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(470, 470, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(471, 471, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(472, 472, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(473, 473, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(474, 474, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(475, 475, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(476, 476, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(477, 477, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 長野県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(478, 478, 1, 1550, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(479, 479, 1, 3100, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(480, 480, 1, 3130, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(481, 481, 1, 7150, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(482, 482, 1, 1580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(483, 483, 1, 3160, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(484, 484, 1, 7180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(485, 485, 1, 5600, timestamp '9999-12-31 00:00:00');

-- 岐阜県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(486, 486, 1, 1420, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(487, 487, 1, 2840, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(488, 488, 1, 2900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(489, 489, 1, 6920, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(490, 490, 1, 1480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(491, 491, 1, 2960, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(492, 492, 1, 6980, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(493, 493, 1, 5500, timestamp '9999-12-31 00:00:00');

-- 静岡県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(494, 494, 1, 1420, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(495, 495, 1, 2840, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(496, 496, 1, 2900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(497, 497, 1, 6920, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(498, 498, 1, 1480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(499, 499, 1, 2960, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(500, 500, 1, 6980, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(501, 501, 1, 5500, timestamp '9999-12-31 00:00:00');

-- 愛知県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(502, 502, 1, 1420, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(503, 503, 1, 2840, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(504, 504, 1, 2900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(505, 505, 1, 6920, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(506, 506, 1, 1480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(507, 507, 1, 2960, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(508, 508, 1, 6980, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(509, 509, 1, 5500, timestamp '9999-12-31 00:00:00');

-- 三重県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(510, 510, 1, 1420, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(511, 511, 1, 2840, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(512, 512, 1, 2900, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(513, 513, 1, 6920, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(514, 514, 1, 1480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(515, 515, 1, 2960, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(516, 516, 1, 6980, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(517, 517, 1, 5500, timestamp '9999-12-31 00:00:00');

-- 滋賀県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(518, 518, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(519, 519, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(520, 520, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(521, 521, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(522, 522, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(523, 523, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(524, 524, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(525, 525, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 京都府
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(526, 526, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(527, 527, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(528, 528, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(529, 529, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(530, 530, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(531, 531, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(532, 532, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(533, 533, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 大阪府
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(534, 534, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(535, 535, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(536, 536, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(537, 537, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(538, 538, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(539, 539, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(540, 540, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(541, 541, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 兵庫県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(542, 542, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(543, 543, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(544, 544, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(545, 545, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(546, 546, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(547, 547, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(548, 548, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(549, 549, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 奈良県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(550, 550, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(551, 551, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(552, 552, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(553, 553, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(554, 554, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(555, 555, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(556, 556, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(557, 557, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 和歌山県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(558, 558, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(559, 559, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(560, 560, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(561, 561, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(562, 562, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(563, 563, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(564, 564, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(565, 565, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 鳥取県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(566, 566, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(567, 567, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(568, 568, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(569, 569, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(570, 570, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(571, 571, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(572, 572, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(573, 573, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 島根県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(574, 574, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(575, 575, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(576, 576, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(577, 577, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(578, 578, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(579, 579, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(580, 580, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(581, 581, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 岡山県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(582, 582, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(583, 583, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(584, 584, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(585, 585, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(586, 586, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(587, 587, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(588, 588, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(589, 589, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 広島県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(590, 590, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(591, 591, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(592, 592, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(593, 593, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(594, 594, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(595, 595, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(596, 596, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(597, 597, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 山口県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(598, 598, 1, 1300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(599, 599, 1, 2600, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(600, 600, 1, 2680, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(601, 601, 1, 6700, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(602, 602, 1, 1380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(603, 603, 1, 2760, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(604, 604, 1, 6780, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(605, 605, 1, 5400, timestamp '9999-12-31 00:00:00');

-- 徳島県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(606, 606, 1, 1200, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(607, 607, 1, 2400, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(608, 608, 1, 2480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(609, 609, 1, 6500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(610, 610, 1, 1280, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(611, 611, 1, 2560, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(612, 612, 1, 6580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(613, 613, 1, 5300, timestamp '9999-12-31 00:00:00');

-- 香川県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(614, 614, 1, 1200, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(615, 615, 1, 2400, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(616, 616, 1, 2480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(617, 617, 1, 6500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(618, 618, 1, 1280, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(619, 619, 1, 2560, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(620, 620, 1, 6580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(621, 621, 1, 5300, timestamp '9999-12-31 00:00:00');

-- 愛媛県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(622, 622, 1, 1200, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(623, 623, 1, 2400, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(624, 624, 1, 2480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(625, 625, 1, 6500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(626, 626, 1, 1280, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(627, 627, 1, 2560, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(628, 628, 1, 6580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(629, 629, 1, 5300, timestamp '9999-12-31 00:00:00');

-- 高知県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(630, 630, 1, 1200, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(631, 631, 1, 2400, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(632, 632, 1, 2480, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(633, 633, 1, 6500, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(634, 634, 1, 1280, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(635, 635, 1, 2560, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(636, 636, 1, 6580, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(637, 637, 1, 5300, timestamp '9999-12-31 00:00:00');

-- 福岡県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(638, 638, 1, 1120, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(639, 639, 1, 2240, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(640, 640, 1, 2300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(641, 641, 1, 6320, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(642, 642, 1, 1180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(643, 643, 1, 2360, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(644, 644, 1, 6380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(645, 645, 1, 5200, timestamp '9999-12-31 00:00:00');

-- 佐賀県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(646, 646, 1, 1120, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(647, 647, 1, 2240, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(648, 648, 1, 2300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(649, 649, 1, 6320, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(650, 650, 1, 1180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(651, 651, 1, 2360, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(652, 652, 1, 6380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(653, 653, 1, 5200, timestamp '9999-12-31 00:00:00');

-- 長崎県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(654, 654, 1, 1120, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(655, 655, 1, 2240, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(656, 656, 1, 2300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(657, 657, 1, 6320, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(658, 658, 1, 1180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(659, 659, 1, 2360, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(660, 660, 1, 6380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(661, 661, 1, 5200, timestamp '9999-12-31 00:00:00');

-- 熊本県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(662, 662, 1, 1120, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(663, 663, 1, 2240, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(664, 664, 1, 2300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(665, 665, 1, 6320, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(666, 666, 1, 1180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(667, 667, 1, 2360, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(668, 668, 1, 6380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(669, 669, 1, 5200, timestamp '9999-12-31 00:00:00');

-- 大分県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(670, 670, 1, 1120, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(671, 671, 1, 2240, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(672, 672, 1, 2300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(673, 673, 1, 6320, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(674, 674, 1, 1180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(675, 675, 1, 2360, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(676, 676, 1, 6380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(677, 677, 1, 5200, timestamp '9999-12-31 00:00:00');

-- 宮崎県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(678, 678, 1, 1120, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(679, 679, 1, 2240, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(680, 680, 1, 2300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(681, 681, 1, 6320, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(682, 682, 1, 1180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(683, 683, 1, 2360, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(684, 684, 1, 6380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(685, 685, 1, 5200, timestamp '9999-12-31 00:00:00');

-- 鹿児島県
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(686, 686, 1, 1120, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(687, 687, 1, 2240, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(688, 688, 1, 2300, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(689, 689, 1, 6320, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(690, 690, 1, 1180, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(691, 691, 1, 2360, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(692, 692, 1, 6380, timestamp '9999-12-31 00:00:00');
insert into shipping_fee_history(shipping_fee_history_id, shipping_fee_id, tax_id, fee, valid_until)
  values(693, 693, 1, 5200, timestamp '9999-12-31 00:00:00');

-- 沖縄県
-- no data
