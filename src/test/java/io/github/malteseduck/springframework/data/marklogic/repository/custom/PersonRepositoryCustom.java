package io.github.malteseduck.springframework.data.marklogic.repository.custom;

import java.util.List;

public interface PersonRepositoryCustom {

    List<Person> findAllPersons();
}
