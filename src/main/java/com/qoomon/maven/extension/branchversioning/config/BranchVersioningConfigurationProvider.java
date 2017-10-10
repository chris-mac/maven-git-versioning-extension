package com.qoomon.maven.extension.branchversioning.config;

import com.qoomon.maven.BuildProperties;
import com.qoomon.maven.extension.branchversioning.ExtensionUtil;
import com.qoomon.maven.extension.branchversioning.SessionScopeUtil;
import com.qoomon.maven.extension.branchversioning.config.model.BranchVersionDescription;
import com.qoomon.maven.extension.branchversioning.config.model.Configuration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by qoomon on 30/11/2016.
 */
@Component(role = BranchVersioningConfigurationProvider.class, instantiationStrategy = "singleton")
public class BranchVersioningConfigurationProvider {

    private static final String DEFAULT_BRANCH_VERSION_FORMAT = "${branchName}-SNAPSHOT";

    @Requirement
    private Logger logger;

    @Requirement
    private SessionScope sessionScope;

    private BranchVersioningConfiguration configuration;

    public BranchVersioningConfiguration get() {

        if (configuration == null) {

            Optional<MavenSession> session = SessionScopeUtil.get(sessionScope, MavenSession.class);

            LinkedHashMap<Pattern, String> branchVersionFormatMap = new LinkedHashMap<>();

            File configFile = ExtensionUtil.getConfigFile(session.get().getRequest(), BuildProperties.projectArtifactId());
            if (configFile.exists()) {
                Configuration configurationModel = loadConfiguration(configFile);
                branchVersionFormatMap = generateBranchVersionFormatMap(configurationModel);
            }
            // add default
            branchVersionFormatMap.put(Pattern.compile(".*"), DEFAULT_BRANCH_VERSION_FORMAT);


            configuration = new BranchVersioningConfiguration(branchVersionFormatMap);
        }

        return configuration;
    }


    private Configuration loadConfiguration(File configFile) {
        logger.debug("load config from " + configFile);
        try (FileInputStream configFileInputStream = new FileInputStream(configFile)) {
            Unmarshaller unmarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
            return (Configuration) unmarshaller.unmarshal(configFileInputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private LinkedHashMap<Pattern, String> generateBranchVersionFormatMap(Configuration configuration) {
        LinkedHashMap<Pattern, String> map = new LinkedHashMap<>();
        for (BranchVersionDescription branchVersionDescription : configuration.branches) {
            map.put(
                    Pattern.compile(branchVersionDescription.branchPattern),
                    branchVersionDescription.versionFormat
            );
        }
        return map;
    }

}