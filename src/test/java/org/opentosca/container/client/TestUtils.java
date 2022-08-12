package org.opentosca.container.client;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.common.configuration.FileBasedRepositoryConfiguration;
import org.eclipse.winery.common.configuration.GitBasedRepositoryConfiguration;
import org.eclipse.winery.common.configuration.RepositoryConfigurationObject;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.CsarExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.eclipse.winery.common.Constants.DEFAULT_LOCAL_REPO_NAME;

public class TestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

    private IRepository fetchRepository(RepositoryConfigurationObject.RepositoryProvider provider, Path repositoryInputPath, String remoteUrl) throws GitAPIException, IOException {
        Path repositoryPath = repositoryInputPath;
        LOGGER.info("Testing with repository directory '{}'", repositoryPath);
        boolean isInitializedRepo = false;
        if (!Files.exists(repositoryPath)) {
            Files.createDirectory(repositoryPath);
        }

        if (!Files.exists(repositoryPath.resolve(".git")) && remoteUrl != null && !Files.exists(repositoryPath.resolve(DEFAULT_LOCAL_REPO_NAME).resolve(".git"))) {
            LOGGER.info("No git repository found, cloning repository from " + remoteUrl);
            cloneRepo(repositoryPath, remoteUrl);
        } else {
            if (remoteUrl == null) {
                LOGGER.info("Remote URL is undefined");
                if (Files.exists(repositoryPath.resolve(DEFAULT_LOCAL_REPO_NAME).resolve(".git"))) {
                    LOGGER.info("Found git repo under /workspace in '{}'", repositoryPath);
                    isInitializedRepo = true;
                }
            } else {
                boolean isCorrectRepository;
                try {
                    isCorrectRepository = Git.open(repositoryPath.toFile())
                            .remoteList().call()
                            .stream().anyMatch(remote ->
                                    remote.getURIs().stream().anyMatch(uri -> uri.toASCIIString().equals(remoteUrl))
                            );
                } catch (Exception e) {
                    try {
                        isCorrectRepository = Git.open(repositoryPath.resolve(DEFAULT_LOCAL_REPO_NAME).toFile())
                                .remoteList().call()
                                .stream().anyMatch(remote ->
                                        remote.getURIs().stream().anyMatch(uri -> uri.toASCIIString().equals(remoteUrl))
                                );
                    } catch (Exception e1) {
                        LOGGER.error("Something went badly wrong!", e);
                        isCorrectRepository = false;
                    }
                }
                if (!isCorrectRepository && remoteUrl != null && !remoteUrl.isEmpty()) {
                    repositoryPath = getRepositoryPath(remoteUrl);
                    cloneRepo(repositoryPath, remoteUrl);
                }
            }
        }

        // inject the current path to the repository factory
        if (!isInitializedRepo) {
            RepositoryFactory.reconfigure(
                    new GitBasedRepositoryConfiguration(false, new FileBasedRepositoryConfiguration(repositoryPath, provider))
            );
        } else {
            RepositoryFactory.reconfigure(new FileBasedRepositoryConfiguration(repositoryPath, provider));
        }

        return RepositoryFactory.getRepository();
    }

    public Path fetchCsar(String remoteUrl, QName serviceTemplateId) throws GitAPIException, IOException, AccountabilityException, RepositoryCorruptException, ExecutionException, InterruptedException {
        Path repositoryPath = getRepositoryPath(remoteUrl);
        IRepository repository = fetchRepository(RepositoryConfigurationObject.RepositoryProvider.FILE, repositoryPath, remoteUrl);
        Path csarPath = exportCsarFromRepository(repository, serviceTemplateId);
        return csarPath;
    }

    private Path exportCsarFromRepository(IRepository repository, QName serviceTemplateId) throws IOException, AccountabilityException, RepositoryCorruptException, ExecutionException, InterruptedException {
        CsarExporter exporter = new CsarExporter(repository);
        Path csarFilePath = Files.createTempDirectory(serviceTemplateId.getLocalPart() + "_Test").resolve(serviceTemplateId.getLocalPart() + ".csar");

        Map<String, Object> exportConfiguration = new HashMap<>();
        exporter.writeCsar(new ServiceTemplateId(serviceTemplateId), Files.newOutputStream(csarFilePath), exportConfiguration);
        return csarFilePath;
    }

    private void cloneRepo(Path repositoryPath, String remoteUrl) throws IOException, GitAPIException {
        if (!Files.exists(repositoryPath)) {
            Files.createDirectory(repositoryPath);
        }
        FileUtils.cleanDirectory(repositoryPath.toFile());

        Git.cloneRepository()
                .setURI(remoteUrl)
                .setBare(false)
                .setCloneAllBranches(true)
                .setDirectory(repositoryPath.toFile())
                .call();
    }

    private Path getRepositoryPath(String testRemoteRepositoryUrl) {
        Path repositoryPath;
        String repoSuffix = "";
        if (testRemoteRepositoryUrl != null) {
            String[] split = testRemoteRepositoryUrl.split("/");
            if (split.length > 0) {
                repoSuffix = split[split.length - 1];
            }
        }
        repositoryPath = Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve("opentosca-test-repository-" + repoSuffix);
        LOGGER.info("Using repository path '{}'", repositoryPath);
        return repositoryPath;
    }
}
