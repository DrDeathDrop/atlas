package io.github.drdeathdrop.atlas.resource.inventory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoResourcesTest {

    @Mock
    private ResourceRepository repository;

    @InjectMocks
    private DemoResources demoResources;

    @Test
    void addsEverySampleThatIsMissing() {
        when(repository.existsByCallSign(anyString())).thenReturn(false);

        demoResources.run(null);

        ArgumentCaptor<Resource> saved = ArgumentCaptor.forClass(Resource.class);
        verify(repository, times(22)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Resource::getCallSign)
                .doesNotHaveDuplicates()
                .contains("TEAM-ALPHA", "AMBULANCE-17", "HELICOPTER-02");
    }

    @Test
    void leavesExistingResourcesAlone() {
        when(repository.existsByCallSign(anyString())).thenReturn(true);

        demoResources.run(null);

        verify(repository, never()).save(any());
    }
}
