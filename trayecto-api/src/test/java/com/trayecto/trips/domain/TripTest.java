package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.Kilometers;
import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.api.events.TripCancelled;
import com.trayecto.trips.api.events.TripCompleted;
import com.trayecto.trips.api.events.TripEdited;
import com.trayecto.trips.api.events.TripStarted;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripTest {

    private static final UserId USER = UserId.newId();
    private static final TripName NAME = new TripName("Ida al trabajo");
    private static final BigDecimal RATE = new BigDecimal("0.70");

    @Test
    void start_putsTripInPending_andEmitsTripStarted() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(12345), "photo-url");
        assertThat(trip.isPending()).isTrue();
        assertThat(trip.totalCost()).isEmpty();
        assertThat(trip.endKm()).isEmpty();
        assertThat(trip.pullDomainEvents()).hasSize(1).first().isInstanceOf(TripStarted.class);
    }

    @Test
    void complete_calculatesCost_andEmitsTripCompleted() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(100), null);
        trip.pullDomainEvents(); // descartar TripStarted

        trip.complete(Kilometers.of(150), MultiplierId.newId(), RATE, "end-photo");

        assertThat(trip.isCompleted()).isTrue();
        assertThat(trip.totalCost()).isPresent();
        assertThat(trip.totalCost().get().amount()).isEqualByComparingTo("35.00"); // 50 * 0.70
        List<Object> events = trip.pullDomainEvents();
        assertThat(events).hasSize(1).first().isInstanceOf(TripCompleted.class);
        TripCompleted ev = (TripCompleted) events.getFirst();
        assertThat(ev.distanceKm()).isEqualByComparingTo("50.0");
        assertThat(ev.totalCost().amount()).isEqualByComparingTo("35.00");
    }

    @Test
    void complete_rejectsCancelledTrip() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(100), null);
        trip.cancel();
        trip.pullDomainEvents();

        assertThatThrownBy(() ->
            trip.complete(Kilometers.of(150), MultiplierId.newId(), RATE, null)
        ).isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("trip.cancelled_cannot_complete"));
    }

    @Test
    void complete_rejectsAlreadyCompletedTrip() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(100), null);
        trip.complete(Kilometers.of(150), MultiplierId.newId(), RATE, null);
        trip.pullDomainEvents();

        assertThatThrownBy(() ->
            trip.complete(Kilometers.of(180), MultiplierId.newId(), RATE, null)
        ).isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("trip.already_completed"));
    }

    @Test
    void cancel_isIdempotent_andEmitsOnceTripCancelled() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(100), null);
        trip.pullDomainEvents();

        trip.cancel();
        trip.cancel(); // segunda llamada idempotente

        assertThat(trip.isCancelled()).isTrue();
        assertThat(trip.pullDomainEvents()).hasSize(1).first().isInstanceOf(TripCancelled.class);
    }

    @Test
    void cancel_rejectsCompletedTrip() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(100), null);
        trip.complete(Kilometers.of(150), MultiplierId.newId(), RATE, null);
        trip.pullDomainEvents();

        assertThatThrownBy(trip::cancel)
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("trip.completed_cannot_cancel"));
    }

    @Test
    void edit_recalculatesCostIfCompleted() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(100), null);
        trip.complete(Kilometers.of(150), MultiplierId.newId(), RATE, null);
        Money originalCost = trip.totalCost().orElseThrow();
        trip.pullDomainEvents();

        // Corregir el km final (de 150 a 200).
        trip.edit(new TripName("Ida al trabajo (corregido)"), trip.startKm(), Kilometers.of(200));

        assertThat(trip.totalCost()).isPresent();
        assertThat(trip.totalCost().get().amount()).isEqualByComparingTo("70.00"); // 100 * 0.70
        assertThat(trip.totalCost().get().amount()).isNotEqualByComparingTo(originalCost.amount());
        assertThat(trip.pullDomainEvents()).hasSize(1).first().isInstanceOf(TripEdited.class);
    }

    @Test
    void softDelete_marksAsDeleted_andBlocksFurtherEdits() {
        Trip trip = Trip.start(USER, NAME, Kilometers.of(100), null);
        trip.softDelete();

        assertThat(trip.isDeleted()).isTrue();
        assertThatThrownBy(trip::cancel)
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("trip.deleted"));
    }
}
