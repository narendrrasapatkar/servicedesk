package com.servicedesk.service;

import com.servicedesk.model.*;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportingServiceTest {

    private TicketRepository ticketRepository;
    private UserRepository userRepository;
    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        userRepository = new UserRepository();
        reportingService = new ReportingService(ticketRepository, userRepository);

        userRepository.save(new User("T1", "Tech A", "t1@corp.com", Role.TECHNICIAN, "IT"));
        userRepository.save(new User("T2", "Tech B", "t2@corp.com", Role.TECHNICIAN, "IT"));

        Ticket t1 = new Ticket("TCK-1", "Monitor Issue", "Flickering screen on dock", "U1", Category.HARDWARE, Priority.HIGH);
        t1.setAssignedTechnicianId("T1");
        t1.setStatus(TicketStatus.IN_PROGRESS);

        Ticket t2 = new Ticket("TCK-2", "VPN Issue", "Cannot connect to server", "U2", Category.NETWORK, Priority.CRITICAL);
        t2.setAssignedTechnicianId("T1");
        t2.setStatus(TicketStatus.ASSIGNED);

        Ticket t3 = new Ticket("TCK-3", "Software Crash", "Word crashes on launch", "U3", Category.SOFTWARE, Priority.LOW);
        t3.setStatus(TicketStatus.RESOLVED);

        Ticket t4 = new Ticket("TCK-4", "Access Pass", "Badge access expired", "U4", Category.ACCESS_MANAGEMENT, Priority.MEDIUM);
        t4.setStatus(TicketStatus.CLOSED);

        ticketRepository.save(t1);
        ticketRepository.save(t2);
        ticketRepository.save(t3);
        ticketRepository.save(t4);
    }

    @Test
    @DisplayName("Should calculate total, active, and resolved metrics accurately")
    void shouldCalculateDashboardMetricsAccurately() {
        Map<String, Object> metrics = reportingService.getDashboardMetrics();

        assertEquals(4L, metrics.get("totalTickets"));
        assertEquals(3L, metrics.get("activeTickets")); // Total - Closed (4 - 1 = 3)
        assertEquals(1L, metrics.get("resolvedTickets"));
        assertEquals(1L, metrics.get("closedTickets"));
        assertEquals(2L, metrics.get("activeTechnicians"));
    }

    @Test
    @DisplayName("Should aggregate technician workload correctly")
    void shouldAggregateTechnicianWorkloadCorrectly() {
        Map<String, Long> workload = reportingService.getTechnicianWorkload();

        assertEquals(1, workload.size());
        assertEquals(2L, workload.get("T1"));
    }
}