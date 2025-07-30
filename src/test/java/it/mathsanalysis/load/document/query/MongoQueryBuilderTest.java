package it.mathsanalysis.load.document.query;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MongoQueryBuilderTest {

    @Test
    void totalQueriesShouldIncrement() {
        MongoQueryBuilder<Object> builder = new MongoQueryBuilder<>();
        Map<String, Object> stats = builder.getQueryStats();
        assertEquals(0L, stats.get("total_queries"));

        builder.buildFindByIdQuery("1");
        long afterFirst = (Long) builder.getQueryStats().get("total_queries");
        assertEquals(1L, afterFirst);

        builder.buildInsertQuery(new Object(), null);
        long afterSecond = (Long) builder.getQueryStats().get("total_queries");
        assertEquals(2L, afterSecond);
    }
}
