package io.github.drdeathdrop.atlas.facility.registry;

import io.github.drdeathdrop.atlas.facility.FacilityType;
import io.github.drdeathdrop.atlas.shared.geo.GeoPoints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(repository.findByName(anyString())).thenReturn(Optional.empty());

        demoFacilities.run(null);

        ArgumentCaptor<Facility> saved = ArgumentCaptor.forClass(Facility.class);
        verify(repository, times(8)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Facility::getName).doesNotHaveDuplicates();
        assertThat(saved.getAllValues()).extracting(Facility::getType)
                .contains(FacilityType.HOSPITAL, FacilityType.SHELTER);
    }

    @Test
    void movesAnExistingSampleThatIsInTheWrongPlace() {
        Facility misplaced = new Facility(
                "UMBAL Kaspela", FacilityType.HOSPITAL, "old address", GeoPoints.of(42.1557, 24.7106), 400);
        misplaced.changeOccupancy(120);
        when(repository.findByName(anyString())).thenReturn(Optional.empty());
        when(repository.findByName("UMBAL Kaspela")).thenReturn(Optional.of(misplaced));

        demoFacilities.run(null);

        assertThat(GeoPoints.latitude(misplaced.getLocation())).isCloseTo(42.130002, within(0.000001));
        assertThat(GeoPoints.longitude(misplaced.getLocation())).isCloseTo(24.715984, within(0.000001));
        assertThat(misplaced.getAddress()).isEqualTo("ul. Sofia 64");
        assertThat(misplaced.getOccupancy()).isEqualTo(120);
        verify(repository, times(7)).save(any());
    }

    @Test
    void leavesASampleThatIsAlreadyInTheRightPlaceAlone() {
        Facility correct = new Facility(
                "UMBAL Kaspela", FacilityType.HOSPITAL, "kept address", GeoPoints.of(42.130002, 24.715984), 400);
        when(repository.findByName(anyString())).thenReturn(Optional.empty());
        when(repository.findByName("UMBAL Kaspela")).thenReturn(Optional.of(correct));

        demoFacilities.run(null);

        assertThat(correct.getAddress()).isEqualTo("kept address");
        verify(repository, times(7)).save(any());
    }
}
