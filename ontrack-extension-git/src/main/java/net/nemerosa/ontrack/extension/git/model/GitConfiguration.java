package net.nemerosa.ontrack.extension.git.model;

import net.nemerosa.ontrack.model.support.UserPassword;

import java.util.Optional;

/**
 * Definition of a Git configuration.
 */
public interface GitConfiguration {

    /**
     * Type
     */
    String getType();

    /**
     * Name in the type
     */
    String getName();

    /**
     * Remote URL-ish
     */
    String getRemote();

    /**
     * Credentials
     */
    Optional<UserPassword> getCredentials();

    /**
     * Link to a commit, using {commit} as placeholder
     */
    String getCommitLink();

    /**
     * Link to a file at a given commit, using {commit} and {path} as placeholders
     */
    String getFileAtCommitLink();

    /**
     * Indexation interval
     */
    int getIndexationInterval();

    /**
     * ID to the {@link net.nemerosa.ontrack.extension.issues.model.IssueServiceConfiguration} associated
     * with this repository.
     */
    String getIssueServiceConfigurationIdentifier();

}
