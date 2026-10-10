package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilityType;
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
class DemoFacilitiesTest {

    @Mock
    private FacilityRepository repository;

    @InjectMocks
    private DemoFacilities demoFacilities;

    @Test
    void addsEverySampleThatIsMissing() {
        when(repository.existsByName(anyString())).thenReturn(false);

        demoFacilities.run(null);

        ArgumentCaptor<Facility> saved = ArgumentCaptor.forClass(Facility.class);
        verify(repository, times(8)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Facility::getName).doesNotHaveDuplicates();
        assertThat(saved.getAllValues()).extracting(Facility::getType)
                .contains(FacilityType.HOSPITAL, FacilityType.SHELTER);
    }

    @Test
    void leavesExistingFacilitiesAlone() {
        when(repository.existsByName(anyString())).thenReturn(true);

        demoFacilities.run(null);

        verify(repository, never()).save(any());
    }
}
