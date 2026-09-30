package az.abb.embassyflow.presentation;

import static org.junit.jupiter.api.Assertions.*;
import az.abb.embassyflow.auth.service.AuthService;
import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.notification.dao.repository.NotificationRepository;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.dao.repository.PaymentRepository;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.portal.dto.request.UpdateDocumentStatusRequest;
import az.abb.embassyflow.portal.enums.PortalDocumentStatus;
import az.abb.embassyflow.portal.service.PortalService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:presentation-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","logging.level.root=WARN"})
@ActiveProfiles("presentation")
class PresentationIntegrationTest {
    @Autowired PresentationService service;
    @Autowired PresentationOrderRepository repository;
    @Autowired DocumentOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired AuthService auth;
    @Autowired CustomerService customers;
    @Autowired NotificationRepository notifications;
    @Autowired PortalService portal;
    PresentationService.Draft draft(String type,String destination) {
        return new PresentationService.Draft(type,"italy",destination,"Example institution","en",List.of("visa-azn","account-eur"),Map.of(
            "visa-azn",new PresentationService.Detail("az",true,"GBP","custom","income","2024-01-01","2024-02-01"),
            "account-eur",new PresentationService.Detail("en",true,"RUB","3","expense","","")),true);
    }
    @Test void completeOrderUsesOriginalServicesAndPreservesPresentationOptions() {
        for(String type:List.of("statement","reference")) {
            var draft=draft(type,type.equals("statement")?"other":"embassy");
            var request=new PresentationService.Submission(draft,"demo-success",UUID.randomUUID().toString());
            long before=payments.count();
            var result=service.submit(request,101L);
            assertEquals(type.equals("statement")?5:10,result.price());
            assertEquals(draft.details(),result.details());assertEquals(draft.destination(),result.destination());assertEquals("Example institution",result.recipient());
            assertEquals(before+1,payments.count());
            assertEquals(result,service.submit(request,101L));assertEquals(before+1,payments.count());
            assertTrue(service.list().contains(result));
        }
    }
    @Test void invalidAndDeclinedOrdersDoNotCreateRowsAndCanBeRetried() {
        String key=UUID.randomUUID().toString();long before=orders.count(),paid=payments.count();
        assertThrows(BusinessException.class,()->service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-declined",key),101L));
        assertEquals(before,orders.count());assertEquals(paid,payments.count());
        assertThrows(BusinessException.class,()->service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-success",key),null));
        var success=service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-success",key),101L);assertEquals("paid",success.paymentStatus());
        assertThrows(BusinessException.class,()->service.submit(new PresentationService.Submission(draft("statement","personal"),"demo-success",key),101L));
    }
    @Test void embassyStatusUpdatesBothOriginalAndPresentationOrder() {
        var order=service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-success",UUID.randomUUID().toString()),101L);
        long before=notifications.count();
        assertEquals("completed",service.status(order.id(),"completed").status());
        assertEquals(before+1,notifications.count());
        service.status(order.id(),"completed");
        assertEquals(before+1,notifications.count());
        var entry=repository.findByOrderNumber(order.id()).orElseThrow();
        assertEquals(OrderStatus.COMPLETED,orders.findById(entry.orderId).orElseThrow().getStatus());
        assertEquals("completed",service.list().stream().filter(o->o.id().equals(order.id())).findFirst().orElseThrow().status());
    }
    @Test void statusEndpointReflectsAChangeThePortalAlreadyMade() {
        var order=service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-success",UUID.randomUUID().toString()),101L);
        // The real portal acts first, so the presentation must not drive a second transition.
        portal.updateStatus(order.id(),new UpdateDocumentStatusRequest(PortalDocumentStatus.REJECTED,"Portal rədd etdi"),1L);
        assertEquals("rejected",service.status(order.id(),"rejected").status());
        var entry=repository.findByOrderNumber(order.id()).orElseThrow();
        assertEquals(OrderStatus.REJECTED,orders.findById(entry.orderId).orElseThrow().getStatus());
    }
    @Test void allUpstreamFinCodesCanCompleteAnOrder() {
        for(String fin:PresentationData.FIN_CODES) {
            var customer=auth.verifyFin(new az.abb.embassyflow.auth.dto.request.FinVerifyRequest(fin));
            var products=customers.accountsFor(customer.customerId(),customer.customerId());
            assertFalse(products.isEmpty());
            String productId="account-"+products.getFirst().id();
            var details=Map.of(productId,new PresentationService.Detail("en",false,"RUB","1","all","",""));
            var draft=new PresentationService.Draft("reference","italy","embassy","","en",List.of(productId),details,true);
            var result=service.submit(new PresentationService.Submission(draft,"demo-success",UUID.randomUUID().toString()),customer.customerId());
            assertEquals(customer.fullName(),result.customer());
            assertEquals("paid",result.paymentStatus());
        }
    }
    @Test @Transactional void paidOrderIsDeliveredBeforeThePresentationShowsItAsPending() {
        var order=service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-success",UUID.randomUUID().toString()),101L);
        var entry=repository.findByOrderNumber(order.id()).orElseThrow();
        var real=orders.findById(entry.orderId).orElseThrow();
        assertEquals("pending",order.status());
        assertEquals("DELIVERED",order.realStatus());
        assertEquals(OrderStatus.DELIVERED,real.getStatus());
        assertEquals(List.of("ORDER_RECEIVED","OTP_VERIFIED","PAYMENT_RECEIVED","ABB_APPROVED","DIGITALLY_SIGNED","DELIVERED_TO_EMBASSY"),
                real.getTimeline().stream().map(step->step.getStep().name()).toList());
    }
    @Test @Transactional void statusEndpointCannotSkipTheDeliveredStep() {
        var order=service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-success",UUID.randomUUID().toString()),101L);
        var entry=repository.findByOrderNumber(order.id()).orElseThrow();
        orders.findById(entry.orderId).orElseThrow().setStatus(OrderStatus.SIGNED);
        assertThrows(BusinessException.class,()->service.status(order.id(),"completed"));
        assertEquals(OrderStatus.SIGNED,orders.findById(entry.orderId).orElseThrow().getStatus());
    }
    @Test void rejectionRequiresGoingThroughThePortalAndKeepsTheNote() {
        var order=service.submit(new PresentationService.Submission(draft("reference","embassy"),"demo-success",UUID.randomUUID().toString()),101L);
        assertEquals("rejected",service.status(order.id(),"rejected").status());
        var entry=repository.findByOrderNumber(order.id()).orElseThrow();
        var real=orders.findById(entry.orderId).orElseThrow();
        assertEquals(OrderStatus.REJECTED,real.getStatus());
        assertEquals("Demo tərəfindən rədd edildi",real.getRejectionNote());
    }
}
