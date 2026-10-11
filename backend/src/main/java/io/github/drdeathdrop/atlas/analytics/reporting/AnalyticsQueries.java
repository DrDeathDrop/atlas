package io.github.drdeathdrop.atlas.analytics.reporting;

import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.Count;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.DayCount;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.Hotspot;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.TimeStat;
import io.github.drdeathdrop.atlas.analytics.AnalyticsOverview.Totals;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public class AnalyticsQueries {
    private static final String IN_SCOPE = """
            i.created_at >= :since
            and (cast(:includeSimulated as boolean)
                 or not exists (select 1 from simulation_incidents s where s.incident_id = i.id))
            """;

    private static final RowMapper<Count> COUNT =
            (row, index) -> new Count(row.getString("name"), row.getLong("total"));

    private final NamedParameterJdbcTemplate jdbc;

    public AnalyticsQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Totals totals(Instant since, boolean includeSimulated) {
        Long simulated = jdbc.queryForObject("""
                select count(*)
                from incidents i
                where i.created_at >= :since
                  and exists (select 1 from simulation_incidents s where s.incident_id = i.id)
                """, scope(since, includeSimulated), Long.class);

        return jdbc.queryForObject("""
                select count(*) as reported,
                       count(*) filter (where i.status in ('REPORTED', 'VERIFIED', 'ACTIVE', 'CONTAINED')) as open,
                       count(*) filter (where i.status in ('RESOLVED', 'REJECTED', 'ARCHIVED')) as closed
                from incidents i
                where\s""" + IN_SCOPE,
                scope(since, includeSimulated),
                (row, index) -> new Totals(
                        row.getLong("reported"),
                        row.getLong("open"),
                        row.getLong("closed"),
                        simulated == null ? 0 : simulated));
    }

    public List<Count> incidentsBy(Grouping grouping, Instant since, boolean includeSimulated) {
        return jdbc.query("""
                select i.%1$s as name, count(*) as total
                from incidents i
                where %2$s
                group by i.%1$s
                order by total desc, name
                """.formatted(grouping.column, IN_SCOPE), scope(since, includeSimulated), COUNT);
    }

    public List<DayCount> incidentsPerDay(LocalDate firstDay, LocalDate lastDay, Instant since,
                                          boolean includeSimulated) {
        MapSqlParameterSource parameters = scope(since, includeSimulated)
                .addValue("firstDay", firstDay.toString())
                .addValue("lastDay", lastDay.toString());

        return jdbc.query("""
                select to_char(d.day, 'YYYY-MM-DD') as day, count(i.id) as total
                from generate_series(cast(:firstDay as date), cast(:lastDay as date), interval '1 day') as d(day)
                left join incidents i
                       on cast(i.created_at at time zone 'UTC' as date) = cast(d.day as date)
                      and\s""" + IN_SCOPE + """
                group by d.day
                order by d.day
                """, parameters, (row, index) -> new DayCount(row.getString("day"), row.getLong("total")));
    }

    public TimeStat timeToStatus(String status, Instant since, boolean includeSimulated) {
        return jdbc.queryForObject("""
                select count(*) as samples,
                       avg(t.minutes) as average,
                       percentile_cont(0.5) within group (order by t.minutes) as median
                from (
                    select cast(extract(epoch from (min(a.occurred_at) - i.created_at)) / 60.0 as double precision)
                               as minutes
                    from incidents i
                    join audit_log a
                      on a.entity_id = i.id
                     and a.action = 'INCIDENT_STATUS_CHANGED'
                     and a.new_state = :status
                    where\s""" + IN_SCOPE + """
                    group by i.id, i.created_at
                ) t
                """, scope(since, includeSimulated).addValue("status", status), AnalyticsQueries::timeStat);
    }

    public TimeStat timeToFirstDispatch(Instant since, boolean includeSimulated) {
        return jdbc.queryForObject("""
                select count(*) as samples,
                       avg(t.minutes) as average,
                       percentile_cont(0.5) within group (order by t.minutes) as median
                from (
                    select cast(extract(epoch from (min(g.assigned_at) - i.created_at)) / 60.0 as double precision)
                               as minutes
                    from incidents i
                    join assignments g on g.incident_id = i.id
                    where\s""" + IN_SCOPE + """
                    group by i.id, i.created_at
                ) t
                """, scope(since, includeSimulated), AnalyticsQueries::timeStat);
    }

    public List<Count> resourcesByStatus() {
        return jdbc.query("""
                select r.status as name, count(*) as total
                from resources r
                group by r.status
                order by total desc, name
                """, new MapSqlParameterSource(), COUNT);
    }

    public List<Count> busiestResources(Instant since, int limit) {
        return jdbc.query("""
                select r.call_sign as name, count(*) as total
                from assignments g
                join resources r on r.id = g.resource_id
                where g.assigned_at >= :since
                group by r.call_sign
                order by total desc, name
                limit :limit
                """, new MapSqlParameterSource("since", Timestamp.from(since)).addValue("limit", limit), COUNT);
    }

    public List<Hotspot> hotspots(Instant since, boolean includeSimulated, double cellDegrees, int limit) {
        MapSqlParameterSource parameters = scope(since, includeSimulated)
                .addValue("cell", cellDegrees)
                .addValue("limit", limit);

        return jdbc.query("""
                select ST_Y(ST_Centroid(ST_Collect(i.location))) as latitude,
                       ST_X(ST_Centroid(ST_Collect(i.location))) as longitude,
                       count(*) as total
                from incidents i
                where\s""" + IN_SCOPE + """
                group by ST_SnapToGrid(i.location, cast(:cell as double precision))
                having count(*) >= 2
                order by total desc
                limit :limit
                """, parameters, (row, index) -> new Hotspot(
                row.getDouble("latitude"), row.getDouble("longitude"), row.getLong("total")));
    }

    public enum Grouping {
        CATEGORY("category"),
        SEVERITY("severity"),
        STATUS("status");

        private final String column;

        Grouping(String column) {
            this.column = column;
        }
    }

    private static MapSqlParameterSource scope(Instant since, boolean includeSimulated) {
        return new MapSqlParameterSource()
                .addValue("since", Timestamp.from(since))
                .addValue("includeSimulated", includeSimulated);
    }

    private static TimeStat timeStat(ResultSet row, int index) throws SQLException {
        return new TimeStat(row.getLong("samples"), number(row, "average"), number(row, "median"));
    }

    private static Double number(ResultSet row, String column) throws SQLException {
        Object value = row.getObject(column);
        return value == null ? null : ((Number) value).doubleValue();
    }
}
