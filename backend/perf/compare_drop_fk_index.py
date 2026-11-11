"""
Interleaved before/after comparison for dropping the single-column foreign-key index on
tasks.project_id (V5). DEV / MEASUREMENT ONLY, same throwaway container as measure_task_list.py.

Machine speed drifts over minutes, so two separate before/after runs can disagree. This script
alternates the two states ten times, flipping which one goes first each round, so every
"with the old index" block has a "without" block that ran seconds away from it.

    python backend/perf/compare_drop_fk_index.py

Per block: 3 warm-up + 30 timed runs of the query, server time from performance_schema.
"""
import statistics
import subprocess

CONTAINER = "taskforge-perf"
MYSQL = ["docker", "exec", "-i", CONTAINER, "mysql", "-uroot", "-pperfroot", "perfdb",
         "--batch", "--skip-column-names", "--comments"]
V5 = open("backend/src/main/resources/db/migration/V5__drop_redundant_tasks_project_index.sql", encoding="utf-8").read()
ROUNDS = 10
WARMUPS = 3
RUNS = 30

LIST_SQL = ("select t1_0.id,t1_0.assignee_id,t1_0.created_at,t1_0.description,t1_0.due_date,"
            "t1_0.priority,t1_0.project_id,t1_0.status,t1_0.title from tasks t1_0 "
            "left join projects p1_0 on p1_0.id=t1_0.project_id where p1_0.id=X'{pid}'{extra}")


def mysql(sql, discard=False):
    r = subprocess.run(MYSQL, input=sql.encode(), stdout=subprocess.DEVNULL if discard else subprocess.PIPE,
                       stderr=subprocess.PIPE)
    if r.returncode:
        raise RuntimeError(r.stderr.decode("utf-8", "replace"))
    return "" if discard else r.stdout.decode("utf-8", "replace")


def set_state(with_old_index):
    if with_old_index:
        has = mysql("select count(*) from information_schema.statistics where table_schema=database() "
                    "and table_name='tasks' and index_name='FKsfhn82y57i3k9uxww1s007acc'").strip()
        if has == "0":
            mysql("create index FKsfhn82y57i3k9uxww1s007acc on tasks (project_id)")
    else:
        mysql(V5)
    mysql("analyze table tasks", discard=True)


def block_median(sql):
    mysql("truncate table performance_schema.events_statements_history_long")
    mysql("\n".join([f"/* w */ {sql};"] * WARMUPS + [f"/* r */ {sql};"] * RUNS), discard=True)
    out = mysql("select timer_wait/1000000000 from performance_schema.events_statements_history_long "
                "where sql_text like '/* r */%'")
    times = [float(x) for x in out.split()]
    assert len(times) == RUNS, len(times)
    return statistics.median(times)


def main():
    mysql("update performance_schema.setup_consumers set enabled='YES' where name='events_statements_history_long'")
    big = mysql("select hex(id) from projects where name='Perf project 1'").strip()
    queries = {"A big project, all tasks": LIST_SQL.format(pid=big, extra=""),
               "B big project, status=DONE": LIST_SQL.format(pid=big, extra=" and t1_0.status='DONE'")}
    result = {q: {"with": [], "without": []} for q in queries}
    for rnd in range(ROUNDS):
        order = ["with", "without"] if rnd % 2 == 0 else ["without", "with"]
        for state in order:
            set_state(state == "with")
            for q, sql in queries.items():
                result[q][state].append(block_median(sql))

    lines = [f"== interleaved: {ROUNDS} rounds x 2 states, {RUNS} timed runs per block (median per block) ==",
             mysql("select concat('MySQL ', version(), ', tasks=', (select count(*) from tasks))").strip()]
    for q, d in result.items():
        w, wo = d["with"], d["without"]
        faster = sum(1 for a, b in zip(w, wo) if b <= a)
        lines += ["", f"--- {q}",
                  f"with old index   : median of block medians {statistics.median(w):.2f} ms  "
                  f"(blocks: {', '.join(f'{x:.1f}' for x in w)})",
                  f"without (V5)     : median of block medians {statistics.median(wo):.2f} ms  "
                  f"(blocks: {', '.join(f'{x:.1f}' for x in wo)})",
                  f"change           : {100 * (statistics.median(wo) / statistics.median(w) - 1):+.1f}%   "
                  f"without was at least as fast in {faster} of {ROUNDS} rounds"]
    set_state(False)
    text = "\n".join(lines)
    print(text)
    open("backend/perf/results-drop-fk-interleaved.txt", "w", encoding="utf-8").write(text + "\n")


main()
