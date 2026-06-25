# -*- coding: utf-8 -*-
"""
将 RSIndex.txt（Unihan Radical-Stroke Index）解析为字符，并按 Unicode 码点
所属范围归入 t_char_dict 已存在的分类：
    - HANZI  : CJK 统一表意符号（CJK Unified Ideographs 及其各扩展区段，
                含 CJK Compatibility Ideographs）
    - DIGIT  : 十进制数字 0-9
    - LETTER : 基本拉丁字母 A-Z / a-z
    - SYMBOL : 其它可打印 ASCII 符号

不在上述四类内的码点将直接舍弃。
入库遵循"已存在则跳过"的原则（基于 char_value 唯一性）。

默认连接本地开发库（127.0.0.1:3306, root/123456, handwrite），可通过
命令行参数覆盖。
"""

from __future__ import annotations

import argparse
import os
import sys
from collections import Counter
from typing import Iterable, Optional

try:
    import pymysql
except ImportError:
    sys.stderr.write("缺少依赖 pymysql，请先 `pip install pymysql`\n")
    raise


# ---------------- Unicode 范围 → 分类 ----------------
# 区间均为闭区间。允许码点 0xF900-0xFAFF 与 0x2F800-0x2FA1F
# 这类 CJK 兼容字也归入 HANZI。
HANZI_RANGES: tuple[tuple[int, int], ...] = (
    (0x3400, 0x4DBF),     # CJK Unified Ideographs Extension A
    (0x4E00, 0x9FFF),     # CJK Unified Ideographs
    (0xFA0E, 0xFA0F),
    (0xFA11, 0xFA11),
    (0xFA13, 0xFA14),
    (0xFA1F, 0xFA1F),
    (0xFA21, 0xFA21),
    (0xFA23, 0xFA24),
    (0xFA27, 0xFA29),
    (0x20000, 0x2A6DF),   # CJK Unified Ideographs Extension B
    (0x2A700, 0x2B73F),   # CJK Unified Ideographs Extension C
    (0x2B740, 0x2B81F),   # CJK Unified Ideographs Extension D
    (0x2B820, 0x2CEAF),   # CJK Unified Ideographs Extension E
    (0x2CEB0, 0x2EBEF),   # CJK Unified Ideographs Extension F
    (0x2EBF0, 0x2EE5D),   # CJK Unified Ideographs Extension I
    (0x30000, 0x3134A),   # CJK Unified Ideographs Extension G
    (0x31350, 0x323AF),   # CJK Unified Ideographs Extension H
    (0x32000, 0x32FFF),   # CJK Unified Ideographs Extension J (Unicode 17.0)
    (0x33000, 0x33479),   # CJK Unified Ideographs Extension K (Unicode 17.0)
    (0xF900, 0xFA6D),     # CJK Compatibility Ideographs
    (0xFA70, 0xFAD9),     # CJK Compatibility Ideographs
    (0x2F800, 0x2FA1D),   # CJK Compatibility Ideographs Supplement
)
DIGIT_RANGES: tuple[tuple[int, int], ...] = (
    (0x0030, 0x0039),     # ASCII 0-9
)
LETTER_RANGES: tuple[tuple[int, int], ...] = (
    (0x0041, 0x005A),     # A-Z
    (0x0061, 0x007A),     # a-z
)
SYMBOL_RANGES: tuple[tuple[int, int], ...] = (
    (0x0021, 0x002F),     # ! " # $ % & ' ( ) * + , - . /
    (0x003A, 0x0040),     # : ; < = > ? @
    (0x005B, 0x0060),     # [ \ ] ^ _ `
    (0x007B, 0x007E),     # { | } ~
)


def classify_codepoint(cp: int) -> Optional[str]:
    """根据 Unicode 码点返回数据库中存在的分类；不在四类内则返回 None。"""
    for lo, hi in HANZI_RANGES:
        if lo <= cp <= hi:
            return "HANZI"
    for lo, hi in DIGIT_RANGES:
        if lo <= cp <= hi:
            return "DIGIT"
    for lo, hi in LETTER_RANGES:
        if lo <= cp <= hi:
            return "LETTER"
    for lo, hi in SYMBOL_RANGES:
        if lo <= cp <= hi:
            return "SYMBOL"
    return None


# ---------------- RSIndex.txt 解析 ----------------

def parse_rsindex(path: str) -> Iterable[int]:
    """逐行解析 RSIndex.txt，产出所有 Unicode 码点。"""
    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            # 格式: radical.additionalStrokes \t U+XXXX U+YYYY ... [# comment]
            tab = line.find("\t")
            if tab < 0:
                continue
            codepoint_field = line[tab + 1:].split("#", 1)[0]
            for tok in codepoint_field.split():
                if not tok.startswith("U+"):
                    continue
                try:
                    yield int(tok[2:], 16)
                except ValueError:
                    continue


# ---------------- 主流程 ----------------

def main() -> int:
    parser = argparse.ArgumentParser(description="将 RSIndex.txt 解析后落库到 t_char_dict。")
    parser.add_argument("--file", default=os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "RSIndex.txt"
    ), help="RSIndex.txt 路径（默认脚本同目录下）")
    parser.add_argument("--host", default=os.environ.get("HANDWRITE_DB_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("HANDWRITE_DB_PORT", "3306")))
    parser.add_argument("--user", default=os.environ.get("HANDWRITE_DB_USERNAME", "root"))
    parser.add_argument("--password", default=os.environ.get("HANDWRITE_DB_PASSWORD", "123456"))
    parser.add_argument("--database", default=os.environ.get("HANDWRITE_DB_NAME", "handwrite"))
    parser.add_argument("--ssl", action="store_true", help="启用 SSL 连接（Azure 等强制 TLS 时使用）")
    parser.add_argument("--batch", type=int, default=500, help="每批插入条数")
    parser.add_argument("--dry-run", action="store_true", help="仅解析不入库")
    args = parser.parse_args()

    if not os.path.isfile(args.file):
        sys.stderr.write(f"找不到文件: {args.file}\n")
        return 2

    # 1. 解析 + 分类 + 去重
    seen: dict[str, str] = {}        # char -> category
    skipped_unknown = 0
    seen_total = 0
    for cp in parse_rsindex(args.file):
        seen_total += 1
        cat = classify_codepoint(cp)
        if cat is None:
            skipped_unknown += 1
            continue
        ch = chr(cp)
        # 同字符多种分类时，保留先出现的（理论不会发生，留作保险）
        seen.setdefault(ch, cat)

    print(f"[parse] 扫描码点: {seen_total} 个, 命中分类: {len(seen)} 个, 舍去: {skipped_unknown} 个")
    print(f"[parse] 分类分布: {Counter(seen.values())}")

    if args.dry_run:
        return 0
    if not seen:
        print("[parse] 无可入库数据，退出")
        return 0

    # 2. 落库
    conn = pymysql.connect(
        host=args.host, port=args.port, user=args.user, password=args.password,
        database=args.database, charset="utf8mb4", connect_timeout=10,
        ssl={"ssl": True} if args.ssl else None
    )
    try:
        with conn.cursor() as cur:
            # 一次性取出已存在字符，避免逐条 SELECT
            cur.execute("SELECT char_value FROM t_char_dict")
            existing = {row[0] for row in cur.fetchall()}
            print(f"[db ] t_char_dict 现有 {len(existing)} 条")

            to_insert: list[tuple[str, str, int, int]] = []
            now_existing = existing
            for ch, cat in seen.items():
                if ch in now_existing:
                    continue
                to_insert.append((ch, cat, 1, 1))

            print(f"[db ] 待新增 {len(to_insert)} 条（已存在 {len(seen) - len(to_insert)} 条自动跳过）")
            if not to_insert:
                return 0

            sql = (
                "INSERT INTO t_char_dict (char_value, category, difficulty, enabled) "
                "VALUES (%s, %s, %s, %s)"
            )
            inserted = 0
            for i in range(0, len(to_insert), args.batch):
                chunk = to_insert[i:i + args.batch]
                cur.executemany(sql, chunk)
                inserted += len(chunk)
                print(f"[db ] 进度: {inserted}/{len(to_insert)}")
            conn.commit()

            # 落库后实际分类统计
            cur.execute("SELECT category, COUNT(*) FROM t_char_dict GROUP BY category")
            print("[db ] 当前 t_char_dict 分类统计:")
            for cat, cnt in cur.fetchall():
                print(f"        {cat:<8s} {cnt}")
            cur.execute("SELECT COUNT(*) FROM t_char_dict")
            print(f"[db ] 当前 t_char_dict 总条数: {cur.fetchone()[0]}")
    finally:
        conn.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
