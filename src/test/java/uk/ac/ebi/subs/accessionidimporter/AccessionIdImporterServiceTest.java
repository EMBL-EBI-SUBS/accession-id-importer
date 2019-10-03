package uk.ac.ebi.subs.accessionidimporter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.AccessionIdImporterApplication;
import uk.ac.ebi.subs.accessionidimporter.utils.MongoDBDependentTest;
import uk.ac.ebi.subs.data.submittable.Submittable;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.accession.AccessionIdWrapper;
import uk.ac.ebi.subs.repository.repos.AccessionIdRepository;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AccessionIdImporterApplication.class)
public class AccessionIdImporterServiceTest {

    private static final int NUMBER_OF_SUBMISSIONS_NOT_IN_ACCESSIONIDWRAPPER = 10;
    private static final int NUMBER_OF_SUBMISSIONS_ALREADY_IN_ACCESSIONIDWRAPPER = 5;

    private List<Submission> submissions = new ArrayList<>();

    @Autowired private AccessionIdImporterService accessionIdImporterService;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private SampleRepository sampleRepository;
    @Autowired private AccessionIdRepository accessionIdRepository;

    @Before
    public void setup() {
        submissions.addAll(generateSubmissionsWithoutAccessionIdWrapper(NUMBER_OF_SUBMISSIONS_NOT_IN_ACCESSIONIDWRAPPER));
        submissions.addAll(generateSubmissionsWithoutAccessionsAndWithoutAccessionIdWrapper(NUMBER_OF_SUBMISSIONS_NOT_IN_ACCESSIONIDWRAPPER));
        submissions.addAll(generateSubmissionsWithAccessionIdWrapper(NUMBER_OF_SUBMISSIONS_ALREADY_IN_ACCESSIONIDWRAPPER));
    }

    @After
    public void tearDown() {
        submissionRepository.deleteAll();
        accessionIdRepository.deleteAll();
    }

    @Test
    public void returnsAllSubmissionIdThatNotExistInAccessionIdWrapper() {
        assertThat(
                accessionIdImporterService.getSubmissionIdsNotExistsInAccessionIdWrapper().size(), is(equalTo(10)));
    }

    @Test
    public void populateAccessionIdRepositoryWithMissingSubmissionsAccessionData() {
        List<String> submissionIds = accessionIdImporterService.getSubmissionIdsNotExistsInAccessionIdWrapper();
        accessionIdImporterService.persistMissingSubmissionData(submissionIds);

        submissionIds.forEach(submissionId -> {
            assertThat(accessionIdRepository.findBySubmissionId(submissionId), notNullValue());
        });
    }

    private List<Submission> generateSubmissionsWithoutAccessionsAndWithoutAccessionIdWrapper(int numberOfSubmissionsNotInAccessionidwrapper) {
        List<Submission> submissionsWithoutAccessionIdWrapper = new ArrayList<>();
        for (int i = 0; i < numberOfSubmissionsNotInAccessionidwrapper; i++) {
            Submission submission = generateSubmission();
            generateSampleAndProjectToSubmission(submission, false);

            submissionsWithoutAccessionIdWrapper.add(submission);
        }

        return submissionsWithoutAccessionIdWrapper;
    }

    private List<Submission> generateSubmissionsWithoutAccessionIdWrapper(int numberOfSubmissionsNotInAccessionidwrapper) {
        List<Submission> submissionsWithoutAccessionIdWrapper = new ArrayList<>();
        for (int i = 0; i < numberOfSubmissionsNotInAccessionidwrapper; i++) {
            Submission submission = generateSubmission();
            generateSampleAndProjectToSubmission(submission, true);

            submissionsWithoutAccessionIdWrapper.add(submission);
        }
        
        return submissionsWithoutAccessionIdWrapper;
    }

    private List<Submission> generateSubmissionsWithAccessionIdWrapper(int numberOfSubmissionsInAccessionidwrapper) {
        List<Submission> submissionsWithAccessionIdWrapper = new ArrayList<>();
        for (int i = 0; i < numberOfSubmissionsInAccessionidwrapper; i++) {
            Submission submission = generateSubmission();
            generateSampleAndProjectToSubmission(submission, true);
            addSubmissionToAccessionIdWrapper(submission);

            submissionsWithAccessionIdWrapper.add(submission);
        }

        return submissionsWithAccessionIdWrapper;
    }

    private Submission generateSubmission() {
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID().toString());
        submission.setName("Test_" + submission.getId());

        submissionRepository.save(submission);

        return submission;
    }

    private void generateSampleAndProjectToSubmission(Submission submission, boolean isAccessioned) {
        Sample sample = new Sample();
        sample.setId(UUID.randomUUID().toString());
        sample.setSubmission(submission);
        if (isAccessioned) {
            setAccession(sample, "SAME" + ThreadLocalRandom.current().nextInt(10000, 9999999));
        }
        sampleRepository.save(sample);

        Project project = new Project();
        project.setId(UUID.randomUUID().toString());
        project.setSubmission(submission);
        project.setAccession("S-SUBS" + ThreadLocalRandom.current().nextInt(1000, 999999));
        projectRepository.save(project);
    }

    private void setAccession(Submittable submittable, String accession) {
        submittable.setAccession(accession);
    }

    private void addSubmissionToAccessionIdWrapper(Submission submission) {
        AccessionIdWrapper accessionIdWrapper = new AccessionIdWrapper();
        accessionIdWrapper.setId(UUID.randomUUID().toString());
        final String submissionId = submission.getId();
        accessionIdWrapper.setSubmissionId(submissionId);

        final Project project = projectRepository.findOneBySubmissionId(submissionId);
        if (project != null) {
            accessionIdWrapper.setBioStudiesAccessionId(project.getAccession());
        }

        final List<Sample> samples = sampleRepository.findBySubmissionId(submissionId);
        if (samples.size() > 0) {
            accessionIdWrapper.setBioSamplesAccessionIds(samples.stream()
                            .map(Sample::getAccession)
                            .collect(Collectors.toList())
            );
        }

        accessionIdRepository.save(accessionIdWrapper);
    }
}
