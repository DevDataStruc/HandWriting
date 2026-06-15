-- V2: 种子数据（角色、字符字典示例）

-- 基础角色
INSERT INTO t_role (code, name, description) VALUES
  ('USER',    '普通用户',  '采集手写体样本的普通用户'),
  ('AUDITOR', '审核员',    '对样本进行审核的工作人员'),
  ('ADMIN',   '管理员',    '系统管理员');

-- 字符字典示例
INSERT INTO t_char_dict (char_value, category, difficulty, description) VALUES
  ('永', 'HANZI',  3, '汉字：永'),
  ('的', 'HANZI',  2, '汉字：的'),
  ('我', 'HANZI',  2, '汉字：我'),
  ('你', 'HANZI',  2, '汉字：你'),
  ('他', 'HANZI',  2, '汉字：他'),
  ('爱', 'HANZI',  3, '汉字：爱'),
  ('中', 'HANZI',  2, '汉字：中'),
  ('国', 'HANZI',  3, '汉字：国'),
  ('0', 'DIGIT',   1, '数字：0'),
  ('1', 'DIGIT',   1, '数字：1'),
  ('2', 'DIGIT',   1, '数字：2'),
  ('3', 'DIGIT',   1, '数字：3'),
  ('4', 'DIGIT',   1, '数字：4'),
  ('5', 'DIGIT',   1, '数字：5'),
  ('6', 'DIGIT',   1, '数字：6'),
  ('7', 'DIGIT',   1, '数字：7'),
  ('8', 'DIGIT',   1, '数字：8'),
  ('9', 'DIGIT',   1, '数字：9'),
  ('A', 'LETTER',  1, '字母：A'),
  ('B', 'LETTER',  1, '字母：B'),
  ('a', 'LETTER',  1, '字母：a'),
  ('b', 'LETTER',  1, '字母：b'),
  ('+', 'SYMBOL',  1, '符号：+'),
  ('-', 'SYMBOL',  1, '符号：-');
