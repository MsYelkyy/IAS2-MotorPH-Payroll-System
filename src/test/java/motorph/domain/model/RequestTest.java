package motorph.domain.model;


import motorph.domain.value.RequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the shared Request approval actions through a concrete subclass.
 */
@DisplayName("Request Tests")
class RequestTest {


    private static final Logger logger = Logger.getLogger(RequestTest.class.getName());


    // approves the request and records the approver
    @Test
    @DisplayName("approve should set status to approved and record the approver")
    void approveShouldSetStatusAndApprover() {
        logger.info("Testing approve sets status and approver.");

        Request request = createPendingRequest();


        request.approve(5, "Looks good");


        assertEquals(RequestStatus.APPROVED, request.getStatus());
        assertEquals(5, request.getApproverAccountId());
        assertEquals("Looks good", request.getRemarks());
    }


    // rejects the request and records the approver
    @Test
    @DisplayName("reject should set status to rejected and record the approver")
    void rejectShouldSetStatusAndApprover() {
        logger.info("Testing reject sets status and approver.");

        Request request = createPendingRequest();


        request.reject(5, "Not allowed");


        assertEquals(RequestStatus.REJECTED, request.getStatus());
        assertEquals(5, request.getApproverAccountId());
        assertEquals("Not allowed", request.getRemarks());
    }


    // cancels the request without a reason
    @Test
    @DisplayName("cancel should set status to cancelled with no remarks")
    void cancelShouldSetStatusWithoutRemarks() {
        logger.info("Testing cancel sets status without remarks.");

        Request request = createPendingRequest();


        request.cancel();


        assertEquals(RequestStatus.CANCELLED, request.getStatus());
        assertNull(request.getRemarks());
    }


    // cancels the request and records the reason
    @Test
    @DisplayName("cancel overload should set status to cancelled and record the reason")
    void cancelOverloadShouldRecordReason() {
        logger.info("Testing cancel overload records the reason.");

        Request request = createPendingRequest();


        request.cancel("Filed by mistake");


        assertEquals(RequestStatus.CANCELLED, request.getStatus());
        assertEquals("Filed by mistake", request.getRemarks());
    }


    // creates a pending request via a concrete subclass
    private Request createPendingRequest() {
        return new OvertimeRequest(
                1, 10001, 1, 1,
                LocalDate.of(2025, 6, 10),
                2.0, "Project deadline",
                LocalDateTime.of(2025, 6, 10, 18, 0)
        );
    }
}
