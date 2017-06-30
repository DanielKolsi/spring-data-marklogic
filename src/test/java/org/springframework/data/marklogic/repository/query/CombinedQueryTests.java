package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryBuilder;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.Person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public class CombinedQueryTests {

    private static final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    @Test
    public void testSerialize() {
        String serialized = combine(qb.and()).serialize();

        assertThat(serialized)
                .contains("<search xmlns=\"http://marklogic.com/appservices/search\">")
                .contains("<query")
                .contains("<and-query/>");
    }

    @Test
    public void testSorted() throws Exception {
        String serialized = combine().sort(new Sort("name")).serialize();
        assertThat(serialized)
                .contains("<search xmlns=\"http://marklogic.com/appservices/search\">")
                .contains("<sort-order direction='ascending'>")
                .contains("<path-index>/name</path-index>");
    }

    @Test
    public void testSortedOnNestedPathWithIndexedAnnotation() throws Exception {
        String serialized = combine()
                .type(Person.class)
                .sort(new Sort("pets/name"))
                .serialize();
        assertThat(serialized)
                .contains("<search xmlns=\"http://marklogic.com/appservices/search\">")
                .contains("<sort-order direction='ascending'>")
                .contains("<path-index>/pets/name</path-index>");
    }

    @Test
    public void testCanDoAndQuery() throws Exception {
        String serialized = combine(
                qb.value(qb.element("name"), "Bob"))
                .and(qb.value(qb.element("age"), 23))
                .serialize();
        assertThat(serialized)
                .contains("<and-query>")
                .contains("<element ns=\"\" name=\"name\"/><text>Bob</text>")
                .contains("<element ns=\"\" name=\"age\"/><text>23</text>");
    }

    @Test
    public void testCanDoOrQuery() throws Exception {
        String serialized = combine(
                qb.value(qb.element("name"), "Bob"))
                .or(qb.value(qb.element("name"), "Fred"))
                .serialize();
        assertThat(serialized)
                .contains("<or-query>")
                .contains("<element ns=\"\" name=\"name\"/><text>Bob</text>")
                .contains("<element ns=\"\" name=\"name\"/><text>Fred</text>");
    }
}
