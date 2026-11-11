"""
Measures the project task-list query on the seeded perf database.
DEV / MEASUREMENT ONLY. Needs the throwaway container from perf/seed_tasks.sql:

    docker run -d --name taskforge-perf -e MYSQL_ROOT_PASSWORD=perfroot -e MYSQL_DATABASE=perfdb -p 3307:3306 mysql:8.0
    (apply the Flyway migrations, run seed_tasks.sql)
    python backend/perf/measure_task_list.py before

For each query it reports, over repeated runs:
  - rows examined / rows sent  (performance_schema statement history)
  - server-side execution time (performance_schema TIMER_WAIT, includes sending the rows)
  - the EXPLAIN plan (index used, estimated rows)
  - EXPLAIN ANALYZE actual total time (median of several runs)
Nothing is estimated: every number printed comes from the database.
"""
import re
import statistics
import subprocess
import sys

CONTAINER = "taskforge-perf"
MYSQL = ["docker", "exec", "-i", CONTAINER, "mysql", "-uroot", "-pperfroot", "perfdb",
         "--batch", "--skip-column-names", "--comments"]
WARMUPS = 3
RUNS = 15
ANALYZE_RUNS = 7

# The exact SQL Hibernate generates for TaskRepository.findByProjectId / findByProjectIdAndStatus
LIST_SQL = ("select t1_0.id,t1_0.assignee_id,t1_0.created_at,t1_0.description,t1_0.due_date,"
            "t1_0.priority,t1_0.project_id,t1_0.status,t1_0.title from tasks t1_0 "
            "left join projects p1_0 on p1_0.id=t1_0.project_id where p1_0.id=X'{pid}'{extra}")
DONE = " and t1_0.status='DONE'"


def mysql(sql, discard_output=False):
    result = subprocess.run(MYSQL, input=sql.encode("utf-8"),
                            stdout=subprocess.DEVNULL if discard_output else subprocess.PIPE,
                            stderr=subprocess.PIPE)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.decode("utf-8", "replace"))
    return "" if discard_output else result.stdout.decode("utf-8", "replace")


def main(label):
    big = mysql("select hex(id) from projects where name='Perf project 1'").strip()
    small = mysql("select hex(id) from projects where name='Perf project 500'").strip()
    queries = {
        "A big project, all tasks": LIST_SQL.format(pid=big, extra=""),
        "B big project, status=DONE": LIST_SQL.format(pid=big, extra=DONE),
        "C small project, all tasks": LIST_SQL.format(pid=small, extra=""),
        "D small project, status=DONE": LIST_SQL.format(pid=small, extra=DONE),
    }

    out = []
    def emit(line=""):
        print(line)
        out.append(line)

    emit(f"== task list measurements: {label} ==")
    emit(mysql("select concat('MySQL ', version())").strip())
    emit("tasks=" + mysql("select count(*) from tasks").strip()
         + " projects=" + mysql("select count(*) from projects").strip()
         + " innodb_buffer_pool_size=" + mysql("select @@innodb_buffer_pool_size").strip())
    emit("indexes on tasks: " + mysql(
        "select group_concat(concat(index_name,'(',cols,')') separator '; ') from "
        "(select index_name, group_concat(column_name order by seq_in_index) cols "
        "from information_schema.statistics where table_schema='perfdb' and table_name='tasks' "
        "group by index_name) x").strip())
    emit(f"{WARMUPS} warm-up runs then {RUNS} timed runs per query; "
         f"EXPLAIN ANALYZE x{ANALYZE_RUNS}; medians reported")
    emit()

    mysql("update performance_schema.setup_consumers set enabled='YES' "
          "where name='events_statements_history_long'; "
          "truncate table performance_schema.events_statements_history_long;")

    script = []
    for name, sql in queries.items():
        tag = name.split()[0]
        script += [f"/* q{tag} warm */ {sql};" for _ in range(WARMUPS)]
        script += [f"/* q{tag} run */ {sql};" for _ in range(RUNS)]
    mysql("\n".join(script), discard_output=True)

    stats = mysql("select substring(sql_text,5,1), rows_examined, rows_sent, timer_wait/1000000000 "
                  "from performance_schema.events_statements_history_long "
                  "where sql_text like '/* q_ run */%' order by event_id")
    per_query = {}
    for line in stats.strip().splitlines():
        tag, examined, sent, ms = line.split("\t")
        per_query.setdefault(tag, []).append((int(examined), int(sent), float(ms)))

    for name, sql in queries.items():
        tag = name.split()[0]
        rows = per_query[tag]
        examined = sorted({r[0] for r in rows})
        sent = sorted({r[1] for r in rows})
        times = [r[2] for r in rows]
        plan = mysql("explain " + sql).strip().splitlines()
        analyze_ms = []
        analyze_sample = None
        for _ in range(ANALYZE_RUNS):
            text = mysql("explain analyze " + sql)
            first = re.search(r"actual time=([\d.]+)\.\.([\d.]+) rows=(\d+)", text)
            analyze_ms.append(float(first.group(2)))
            analyze_sample = text
        emit(f"--- {name}")
        emit(f"rows examined: {examined if len(examined) > 1 else examined[0]}   rows sent: "
             f"{sent if len(sent) > 1 else sent[0]}")
        emit(f"server time ms: median {statistics.median(times):.2f}  min {min(times):.2f}  "
             f"max {max(times):.2f}  (n={len(times)})")
        emit(f"EXPLAIN ANALYZE top-node total ms: median {statistics.median(analyze_ms):.2f}  "
             f"(runs: {', '.join(f'{m:.1f}' for m in analyze_ms)})")
        emit("EXPLAIN (id, select_type, table, partitions, type, possible_keys, key, key_len, ref, rows, filtered, Extra):")
        for row in plan:
            emit("  " + row)
        emit("EXPLAIN ANALYZE sample:")
        for row in analyze_sample.strip().replace("\\n", "\n").splitlines():
            emit("  " + row)
        emit()

    with open(f"backend/perf/results-{label}.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "run")
