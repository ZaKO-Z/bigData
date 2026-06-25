"""
并发基准测试：取前 N 个 URL，测试不同并发数的耗时
用法: python bench_concurrency.py
"""
import sys
import time
import asyncio
from pathlib import Path

_BASE_DIR = Path(__file__).resolve().parent
if str(_BASE_DIR) not in sys.path:
    sys.path.insert(0, str(_BASE_DIR))

import openpyxl
from config import INPUT_FILE
from crawler import CheckpointManager, FailedQueue, crawl_urls_async
from export import CsvAppender
from config import CSV_FIELDS

TEST_URL_COUNT = 30
CONCURRENCY_LEVELS = [1, 2, 3]


def get_test_urls(n=TEST_URL_COUNT):
    file_path = Path(_BASE_DIR) / INPUT_FILE
    wb = openpyxl.load_workbook(file_path)
    ws = wb.active
    urls = []
    for row in range(2, ws.max_row + 1):
        v = ws.cell(row=row, column=1).value
        if v and isinstance(v, str) and v.startswith('http'):
            urls.append(v)
            if len(urls) >= n:
                break
    return urls


async def run_benchmark(concurrency, urls):
    """以指定并发数爬取，返回耗时和成功数"""
    # 每次测试使用独立的 checkpoint/failed/csv（防止跳过）
    suffix = f"_bench_c{concurrency}"
    checkpoint = CheckpointManager(f"checkpoint{suffix}.json")
    failed_queue = FailedQueue(f"failed{suffix}.json")
    csv_appender = CsvAppender(f"data{suffix}.csv", CSV_FIELDS)
    csv_appender.init_header()

    start = time.perf_counter()
    total, failed = await crawl_urls_async(
        urls, checkpoint=checkpoint, failed_queue=failed_queue,
        csv_appender=csv_appender, max_concurrency=concurrency,
    )
    elapsed = time.perf_counter() - start
    csv_appender.finish()

    # 清理临时文件
    for f in [f"checkpoint{suffix}.json", f"failed{suffix}.json", f"data{suffix}.csv"]:
        p = Path(_BASE_DIR) / f
        if p.exists():
            p.unlink()

    return elapsed, total, failed


async def main():
    urls = get_test_urls(TEST_URL_COUNT)
    print(f"\n{'='*60}")
    print(f"并发基准测试 — {len(urls)} 个URL")
    print(f"{'='*60}\n")

    results = []
    for concurrency in CONCURRENCY_LEVELS:
        print(f"测试并发数={concurrency} ... ", end="", flush=True)
        elapsed, total, failed = await run_benchmark(concurrency, urls)
        speed = total / elapsed if elapsed > 0 else 0
        results.append((concurrency, elapsed, total, failed, speed))
        print(f"耗时={elapsed:.1f}s, 数据={total}条, 失败={failed}, 速率={speed:.0f}条/s")

    # 汇总表格
    print(f"\n{'='*60}")
    print(f"{'并发数':<8} {'耗时(s)':<10} {'数据(条)':<10} {'失败':<6} {'速率(条/s)':<12}")
    print(f"{'-'*60}")
    base_time = results[0][1]
    for c, t, total, failed, speed in results:
        ratio = base_time / t if t > 0 else 0
        print(f"{c:<8} {t:<10.1f} {total:<10} {failed:<6} {speed:<12.0f} (x{ratio:.1f})")
    print(f"{'='*60}")
    print(f"基准(并发=1)耗时: {base_time:.1f}s")
    best_c, best_t, _, _, _ = max(results, key=lambda r: r[1])
    print(f"最快: 并发={best_c}, 加速比 x{base_time/best_t:.1f}")
    print()


if __name__ == "__main__":
    asyncio.run(main())
