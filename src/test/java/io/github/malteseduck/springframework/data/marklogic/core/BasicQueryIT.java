package io.github.malteseduck.springframework.data.marklogic.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.StructuredQueryBuilder;
import io.github.malteseduck.springframework.data.marklogic.domain.ChunkRequest;
import io.github.malteseduck.springframework.data.marklogic.domain.facets.FacetResultDto;
import io.github.malteseduck.springframework.data.marklogic.domain.facets.FacetedPage;
import io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder;
import io.github.malteseduck.springframework.data.marklogic.repository.query.QueryTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import io.github.malteseduck.springframework.data.marklogic.DatabaseConfiguration;
import io.github.malteseduck.springframework.data.marklogic.InvalidMarkLogicApiUsageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class)
})
public class BasicQueryIT {

    private MarkLogicTemplate template;
    private StructuredQueryBuilder qb = new StructuredQueryBuilder();

    private Person bobby, george, jane;
    private List<Person> all;

    @Rule
    public ExpectedException expectation = ExpectedException.none();

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
    }

    @Before
    public void init() {
        cleanDb();

        bobby = new Person("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"));
        george = new Person("George", 12, "male", "engineer", "The guy wo works at the gas station, he is your friend", Instant.parse("2016-01-01T00:00:00Z"));
        jane = new Person("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-01-01T00:00:00Z"));

        all = template.write(asList(bobby, george, jane));
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        template.dropCollection(Person.class);
    }

    @Test
    public void testExists() throws Exception {
        assertThat(template.exists(qb.value(qb.jsonProperty("age"), 23), Person.class)).as("does exist").isTrue();
        assertThat(template.exists(qb.value(qb.jsonProperty("occupation"), "knight"), Person.class)).as("doesn't exist").isFalse();
    }

    @Test
    public void testExistsById() throws Exception {
        assertThat(template.exists(bobby.getId(), Person.class)).as("does exist").isTrue();
        assertThat(template.exists("invalidid")).as("doesn't exist").isFalse();
    }

    @Test
    public void testExistsByUri() throws Exception {
        assertThat(template.exists("/Person/" + bobby.getId() + ".json")).as("does exist").isTrue();
        assertThat(template.exists("invalidid")).as("doesn't exist").isFalse();
    }

    @Test
    public void testCount() throws Exception {
        assertThat(template.count(Person.class)).isEqualTo(all.size());
    }

    @Test
    public void testCountByCollections() throws Exception {
        assertThat(template.count("Person")).isEqualTo(all.size());
    }

    @Test
    public void testCountByQuery() throws Exception {
        assertThat(template.count(qb.value(qb.jsonProperty("gender"), "male"))).as("without type").isEqualTo(2);
        assertThat(template.count(qb.value(qb.jsonProperty("gender"), "male"), Person.class)).as("options type").isEqualTo(2);
    }

    @Test
    public void testQueryByValue() {
        List<Person> people = template.search(
            qb.value(qb.jsonProperty("gender"), "male"),
            Person.class
        );

        assertThat(people).hasSize(2);
    }

    @Test
    public void testQueryByValueWithLimit() {
        Page<Person> people = template.search(
            qb.value(qb.jsonProperty("gender"), "male"),
            0,
            1,
            Person.class
        );

        assertThat(people).hasSize(1);
        assertThat(people.getTotalPages()).isEqualTo(2);
    }

    @Test
    public void testSearchOne() {
        Person person = template.searchOne(
                qb.value(qb.jsonProperty("name"), "Bobby"),
                Person.class
        );

        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testSearchOneReturningNoResults() {
        Person person = template.searchOne(
                qb.value(qb.jsonProperty("name"), "BubbaMan"),
                Person.class
        );

        assertThat(person).isNull();
    }

    @Test
    public void testQuerySorted() {
        List<Person> people = template.search(
            template.sortQuery(Sort.by("name"), null),
            Person.class
        );

        assertThat(people).containsExactly(bobby, george, jane);
    }

    @Test
    public void testQueryWithPageable() {
        Page<Person> people = template.search(
                null,
                ChunkRequest.of(0, 3, Sort.by("name")),
                Person.class
        );

        assertThat(people.getContent()).containsExactly(bobby, george, jane);
    }

    @Test
    public void testQueryByValueSorted() {
        List<Person> people = template.search(
            template.sortQuery(
                Sort.by("name"),
                qb.value(qb.jsonProperty("gender"), "male")
            ),
            Person.class
        );

        assertThat(people).containsExactly(bobby, george);
    }

    @Test
    public void testQueryByValueStreamed() throws JsonProcessingException {
        InputStream people = template.stream(
                qb.value(qb.jsonProperty("name"), "Bobby"),
                PersonToStream.class
        );

        assertThat(people).hasSameContentAs(QueryTestUtils.stream(bobby));
    }

    @Test
    public void testQueryByValueStreamedWithPagable() throws JsonProcessingException {
        InputStream people = template.stream(
                qb.value(qb.jsonProperty("gender"), "male"),
                ChunkRequest.of(0, 1, Sort.by("name")),
                PersonToStream.class
        );

        assertThat(people).hasSameContentAs(QueryTestUtils.stream(bobby));
    }

    @Test
    public void testHandleBadSortException() throws Exception {
        expectation.expect(InvalidMarkLogicApiUsageException.class);
        expectation.expectMessage("SEARCH-BADORDERBY");

        template.search(
            template.sortQuery(Sort.by("blabbitybloobloo"), null),
            Person.class
        );
    }

    @Test
    public void testQueryReturningFacets() {
        FacetedPage<Person> results = template.facetedSearch(
                CombinedQueryDefinitionBuilder.combine()
                        .sort(Sort.by("name"))
                        .optionsName("facets"),
                0,
                1,
                Person.class
        );

        assertThat(results.getContent()).contains(bobby);
        assertThat(results.getFacets())
                .extracting(FacetResultDto::getName).contains("occupation", "age", "gender");
        assertThat(results.getFacets())
                .extracting(FacetResultDto::getCount).contains(3L, 3L, 2L);
    }
}
