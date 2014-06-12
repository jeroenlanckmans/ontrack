package net.nemerosa.ontrack.extension.svn.db;

import net.nemerosa.ontrack.repository.support.AbstractJdbcRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class SVNRevisionJdbcDao extends AbstractJdbcRepository implements SVNRevisionDao {

    public static final int MESSAGE_LENGTH = 500;

    @Autowired
    public SVNRevisionJdbcDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getLast(int repositoryId) {
        Long value = getNamedParameterJdbcTemplate().queryForObject(
                "SELECT MAX(REVISION) FROM EXT_SVN_REVISION WHERE REPOSITORY = :repositoryId",
                params("repositoryId", repositoryId),
                Long.class);
        return value != null ? value : 0L;
    }

    @Override
    public void addRevision(int repositoryId, long revision, String author, LocalDateTime dateTime, String message, String branch) {
        NamedParameterJdbcTemplate t = getNamedParameterJdbcTemplate();
        // Getting rid of the revision
        MapSqlParameterSource params = params("revision", revision).addValue("repositoryId", repositoryId);
        t.update("DELETE FROM EXT_SVN_REVISION WHERE REPOSITORY =:repositoryId AND REVISION = :revision", params);
        // Creates the revision record
        t.update("INSERT INTO EXT_SVN_REVISION (REPOSITORY, REVISION, AUTHOR, CREATION, MESSAGE, BRANCH) " +
                        "VALUES (:repositoryId, :revision, :author, :creation, :message, :branch)",
                params
                        .addValue("author", author)
                        .addValue("creation", dateTimeForDB(dateTime))
                        .addValue("message", Objects.toString(StringUtils.abbreviate(message, MESSAGE_LENGTH), ""))
                        .addValue("branch", branch)
        );
    }

    @Override
    public void addMergedRevisions(int repositoryId, long revision, List<Long> mergedRevisions) {
        NamedParameterJdbcTemplate t = getNamedParameterJdbcTemplate();
        for (long mergedRevision : mergedRevisions) {
            t.update("INSERT INTO SVN_EXT_MERGE_REVISION (REPOSITORY, REVISION, TARGET) " +
                            "VALUES (:repository, :mergedRevision, :revision)",
                    params("mergedRevision", mergedRevision)
                            .addValue("repository", repositoryId)
                            .addValue("revision", revision)
            );
        }
    }
}
