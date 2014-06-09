package net.nemerosa.ontrack.extension.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import net.nemerosa.ontrack.extension.jenkins.client.JenkinsConnection;
import net.nemerosa.ontrack.extension.support.configurations.UserPasswordConfiguration;
import net.nemerosa.ontrack.model.form.Form;
import net.nemerosa.ontrack.model.form.Password;
import net.nemerosa.ontrack.model.form.Text;
import net.nemerosa.ontrack.model.support.ConfigurationDescriptor;

import static net.nemerosa.ontrack.model.form.Form.defaultText;

@Data
public class JenkinsConfiguration implements UserPasswordConfiguration<JenkinsConfiguration> {

    private final String name;
    private final String url;
    private final String user;
    private final String password;

    public static Form form() {
        return Form.create()
                .with(defaultText())
                .url()
                .with(Text.of("user").label("User").length(16).optional())
                .with(Password.of("password").label("Password").length(40).optional());
    }

    @Override
    public JenkinsConfiguration obfuscate() {
        return new JenkinsConfiguration(
                name,
                url,
                user,
                ""
        );
    }

    public Form asForm() {
        return form()
                .with(defaultText().readOnly().value(name))
                .fill("url", url)
                .fill("user", user)
                .fill("password", "");
    }

    @Override
    public JenkinsConfiguration withPassword(String password) {
        return new JenkinsConfiguration(
                name,
                url,
                user,
                password
        );
    }

    @Override
    public ConfigurationDescriptor getDescriptor() {
        return new ConfigurationDescriptor(name, name);
    }

    /**
     * Gets the Jenkins connection information
     */
    @JsonIgnore
    public JenkinsConnection getConnection() {
        return new JenkinsConnection(
                url,
                user,
                password
        );
    }
}
