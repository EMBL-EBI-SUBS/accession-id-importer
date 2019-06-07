package uk.ac.ebi.subs.accessionidimporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.accession.AccessionIdWrapper;
import uk.ac.ebi.subs.repository.repos.AccessionIdRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccessionIdImporterService {

    String name;
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessionIdImporterService.class);

    private SubmissionRepository submissionRepository;
    private AccessionIdRepository accessionIdRepository;
    private ProjectRepository projectRepository;
    private SampleRepository sampleRepository;

    public AccessionIdImporterService(SubmissionRepository submissionRepository, AccessionIdRepository accessionIdRepository,
                                      ProjectRepository projectRepository, SampleRepository sampleRepository) {
        this.submissionRepository = submissionRepository;
        this.accessionIdRepository = accessionIdRepository;
        this.projectRepository = projectRepository;
        this.sampleRepository = sampleRepository;
    }

    public void importNotExistingAccessionIds() {
        List<String> submissionIds = getSubmissionIdsNotExistsInAccessionIdWrapper();

        LOGGER.info("AccessionIdImporterService is about to import the following submissions accessionIds: {}",
                submissionIds);

        persistMissingSubmissionData(submissionIds);
    }

    List<String> getSubmissionIdsNotExistsInAccessionIdWrapper() {
        List<String> submissionIds =
                submissionRepository.findAll().stream().map(Submission::getId).collect(Collectors.toList());

        List<String> submissionIdsFromAccessionIdRepo =
                accessionIdRepository.findAll().stream().map(AccessionIdWrapper::getSubmissionId).collect(Collectors.toList());

        submissionIds.removeAll(submissionIdsFromAccessionIdRepo);

        return submissionIds;
    }

    void persistMissingSubmissionData(List<String> submissionIds) {
        List<AccessionIdWrapper> accessionIdWrappersToAdd = new ArrayList<>();

        submissionIds.forEach(submissionId -> {
            String biostudiesAccessionId = null;
            List<String> biosamplesAccessionIds = new ArrayList<>();

            Project project = projectRepository.findOneBySubmissionId(submissionId);
            if (project != null) {
                biostudiesAccessionId = project.getAccession();
            }

            List<Sample> samples = sampleRepository.findBySubmissionId(submissionId);
            if (samples.size() > 0) {
                biosamplesAccessionIds = samples.stream().map(Sample::getAccession).collect(Collectors.toList());
            }

            AccessionIdWrapper accessionIdWrapper = new AccessionIdWrapper();
            accessionIdWrapper.setSubmissionId(submissionId);
            accessionIdWrapper.setBioStudiesAccessionId(biostudiesAccessionId);
            accessionIdWrapper.setBioSamplesAccessionIds(biosamplesAccessionIds);

            accessionIdWrappersToAdd.add(accessionIdWrapper);

            LOGGER.info("New AccessionIdWrapper document is about to add to the collection. [submissionId: {}, biostudiesAccessionId: {}, biosamplesAccessionIds: {}]",
                    submissionId, biostudiesAccessionId, biosamplesAccessionIds);
        });

        accessionIdRepository.save(accessionIdWrappersToAdd);
    }
}
