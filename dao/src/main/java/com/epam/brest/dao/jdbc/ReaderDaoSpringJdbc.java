package com.epam.brest.dao.jdbc;

import com.epam.brest.dao.ReaderDao;
import com.epam.brest.dao.jdbc.tools.ExistReader;
import com.epam.brest.dao.jdbc.tools.FindAllReader;
import com.epam.brest.dao.jdbc.tools.FindAllReadersByDate;
import com.epam.brest.dao.jdbc.tools.FindReaderById;
import com.epam.brest.dao.jdbc.tools.FindReaderByIdWithBooks;
import com.epam.brest.dao.jdbc.tools.SaveReader;
import com.epam.brest.dao.jdbc.tools.UpdateReader;
import com.epam.brest.model.Reader;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository("readerDaoSpringJdbc")
public class ReaderDaoSpringJdbc implements ReaderDao, InitializingBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReaderDaoSpringJdbc.class);

  @Value("${reader.jdbc.findAllReader}")
  private String findAllReaderSql;
  @Value("${reader.jdbc.findReaderById}")
  private String findReaderByIdSql;
  @Value("${reader.jdbc.findReaderByIdWithBooks}")
  private String findReaderByIdWithBooksSql;
  @Value("${reader.jdbc.save}")
  private String saveSql;
  @Value("${reader.jdbc.update}")
  private String updateSql;
  @Value("${reader.jdbc.existAndActiveReader}")
  private String existReaderSql;
  @Value("${reader.jdbc.findAllReadersByDate}")
  private String findAllReadersByDateSql;

  private final DataSource dataSource;

  public ReaderDaoSpringJdbc(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public List<Reader> findAll() {
    LOGGER.info("findAll() was started");
    return new FindAllReader(dataSource, findAllReaderSql).execute();
  }

  @Override
  public Optional<Reader> findReaderById(Integer id) {
    LOGGER.info("findReaderById(id)  was started");
    LOGGER.debug("id={}", id);
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource("readerId", id);
    return Optional.ofNullable((Reader) new FindReaderById(dataSource, findReaderByIdSql).
        findObjectByNamedParam(sqlParameterSource.getValues()));
  }

  @Override
  public Optional<Reader> findReaderByIdWithBooks(Integer id) {
    LOGGER.info("findReaderByIdWithBooks(id) was started");
    LOGGER.debug("id={}", id);
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource("readerId", id);
    NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    FindReaderByIdWithBooks findReaderByIdWithBooks = new FindReaderByIdWithBooks();
    return Optional.ofNullable((Reader) jdbcTemplate.
        query(findReaderByIdWithBooksSql, sqlParameterSource.getValues(),
            findReaderByIdWithBooks));
  }

  @Override
  public Reader save(Reader reader) {
    LOGGER.info("save(reader)  was started");
    LOGGER.debug("reader={}", reader);
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("firstName", reader.getFirstName());
    sqlParameterSource.addValue("lastName", reader.getLastName());
    sqlParameterSource.addValue("patronymic", reader.getPatronymic());
    sqlParameterSource.addValue("dateOfRegistry", reader.getDateOfRegistry());
    KeyHolder keyHolder = new GeneratedKeyHolder();
    new SaveReader(dataSource, saveSql)
        .updateByNamedParam(sqlParameterSource.getValues(), keyHolder);
    reader.setReaderId(Objects.requireNonNull(keyHolder.getKey()).intValue());
    return reader;
  }

  @Override
  public Integer update(Reader reader) {
    LOGGER.info("update(reader) was started");
    LOGGER.debug("reader={}", reader);
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("firstName", reader.getFirstName());
    sqlParameterSource.addValue("lastName", reader.getLastName());
    sqlParameterSource.addValue("patronymic", reader.getPatronymic());
    sqlParameterSource.addValue("dateOfRegistry", reader.getDateOfRegistry());
    sqlParameterSource.addValue("active", reader.isActive());
    sqlParameterSource.addValue("readerId", reader.getReaderId());
    return new UpdateReader(dataSource, updateSql)
        .updateByNamedParam(sqlParameterSource.getValues());
  }

  @Override
  public Integer changeReaderToNoActive(Reader reader) {
    LOGGER.info("changeReaderToNoActive(reader) was started");
    LOGGER.debug("reader={}", reader);
    reader.setActive(false);
    return update(reader);
  }

  @Override
  public Integer changeReaderToActive(Reader reader) {
    LOGGER.info("changeReaderToActive(reader) was started");
    LOGGER.debug("reader={}", reader);
    reader.setActive(true);
    return update(reader);
  }

  @Override
  public Boolean isExistAmongActiveReaders(Integer readerId) {
    LOGGER.info("isExistAmongActiveReaders(reader) was started");
    LOGGER.debug("readerId={}, active=true default", readerId);
    return isExistAmongReadersByActive(readerId, true);
  }

  @Override
  public Boolean isExistAmongReadersByActive(Integer readerId, boolean active) {
    LOGGER.info("isExistAmongReadersByActive(reader, active) was started");
    LOGGER.debug("readerId={}, active={}", readerId, active);
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("readerId", readerId);
    sqlParameterSource.addValue("active", active);
    return new ExistReader(dataSource, existReaderSql).
        findObjectByNamedParam(sqlParameterSource.getValues());
  }

  @Override
  public List<Reader> findAllByDate(LocalDate from, LocalDate to) {
    LOGGER.info("findAllByDate() was started");
    MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
    sqlParameterSource.addValue("from", from);
    sqlParameterSource.addValue("to", to);
    return new FindAllReadersByDate(dataSource, findAllReadersByDateSql)
        .executeByNamedParam(sqlParameterSource.getValues());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (dataSource == null) {
      throw new BeanCreationException("DataSource must not be null in ReaderDaoSpringJdbc.class");
    }
  }
}
