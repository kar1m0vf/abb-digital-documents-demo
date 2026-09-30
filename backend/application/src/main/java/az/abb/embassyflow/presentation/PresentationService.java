package az.abb.embassyflow.presentation;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.customer.service.CustomerService;
import az.abb.embassyflow.embassy.service.PortalUserService;
import az.abb.embassyflow.order.dao.repository.DocumentOrderRepository;
import az.abb.embassyflow.order.dto.request.*;
import az.abb.embassyflow.order.enums.*;
import az.abb.embassyflow.order.service.OrderService;
import az.abb.embassyflow.order.service.PaymentService;
import az.abb.embassyflow.portal.dto.request.UpdateDocumentStatusRequest;
import az.abb.embassyflow.portal.enums.PortalDocumentStatus;
import az.abb.embassyflow.portal.service.PortalService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Profile("presentation")
public class PresentationService {
    public record Detail(String language, boolean equivalent, String equivalentCurrency, String period, String operation, String start, String end) {}
    public record Draft(String type, String embassy, String destination, String recipient, String language, List<String> accounts, Map<String, Detail> details, boolean reviewed) {}
    public record Submission(Draft draft, String paymentToken, String idempotencyKey) {}
    public record OrderView(String id, Long orderId, String customer, String type, String embassy, String destination, String recipient, String language, List<String> accounts, Map<String, Detail> details, boolean reviewed, String date, int price, String status, String paymentStatus, boolean seed, String realStatus) {}
    private final PresentationOrderRepository repository;
    private final DocumentOrderRepository orders;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CustomerService customerService;
    private final PortalService portalService;
    private final PortalUserService portalUserService;
    private final ObjectMapper json;
    public PresentationService(PresentationOrderRepository repository, DocumentOrderRepository orders, OrderService orderService, PaymentService paymentService, CustomerService customerService, PortalService portalService, PortalUserService portalUserService, ObjectMapper json) {
        this.repository=repository; this.orders=orders; this.orderService=orderService; this.paymentService=paymentService; this.customerService=customerService; this.portalService=portalService; this.portalUserService=portalUserService; this.json=json;
    }
    private BusinessException invalid() { return new BusinessException("VALIDATION_ERROR", "error.validation", HttpStatus.BAD_REQUEST); }
    private void validate(Draft d) {
        if (d == null || !Set.of("statement","reference").contains(Objects.toString(d.type(),"")) || !Set.of("az","en").contains(Objects.toString(d.language(),"")) || !d.reviewed() || d.accounts()==null || d.accounts().isEmpty() || d.accounts().size()>3 || new HashSet<>(d.accounts()).size()!=d.accounts().size() || d.details()==null) throw invalid();
        if (!Set.of("embassy","personal","other").contains(Objects.toString(d.destination(),"")) || !PresentationData.EMBASSIES.containsKey(Objects.toString(d.embassy(),"")) || (d.type().equals("reference")&&!d.destination().equals("embassy")) || (d.destination().equals("other")&&(d.recipient()==null||d.recipient().isBlank())) || (d.recipient()!=null&&d.recipient().length()>120)) throw invalid();
        for (String id : d.accounts()) {
            boolean presentationProduct=id!=null&&PresentationData.PRODUCTS.stream().anyMatch(p->p.id().equals(id));
            if (id==null || id.equals("credit") || (!presentationProduct&&!id.matches("(?:account|card)-\\d+"))) throw invalid();
            Detail v=d.details().get(id);
            if(v==null || !Set.of("az","en").contains(Objects.toString(v.language(),"")) || !Set.of("1","3","6","12","custom").contains(Objects.toString(v.period(),"")) || !Set.of("all","income","expense").contains(Objects.toString(v.operation(),"")) || !Set.of("USD","EUR","RUB","GBP").contains(Objects.toString(v.equivalentCurrency(),""))) throw invalid();
            if (v.period().equals("custom")) try { LocalDate start=LocalDate.parse(v.start()), end=LocalDate.parse(v.end()); if(start.isAfter(end)||end.isAfter(LocalDate.now())) throw invalid(); } catch (RuntimeException ex) { throw invalid(); }
        }
    }
    @Transactional public OrderView submit(Submission request, Long customerId) {
        if(customerId==null) throw new BusinessException("UNAUTHORIZED","error.unauthorized",HttpStatus.UNAUTHORIZED);
        var customer=customerService.findById(customerId).orElseThrow(()->new BusinessException("UNAUTHORIZED","error.unauthorized",HttpStatus.UNAUTHORIZED));
        if(request==null || request.idempotencyKey()==null || !request.idempotencyKey().matches("[A-Za-z0-9-]{16,80}")) throw invalid();
        Draft d=request.draft(); validate(d);
        var previous=repository.findByRequestKey(request.idempotencyKey());
        if(previous.isPresent()) {
            OrderView saved=view(previous.get());
            Draft prior=new Draft(saved.type(),saved.embassy(),saved.destination(),saved.recipient(),saved.language(),saved.accounts(),saved.details(),saved.reviewed());
            if (!prior.equals(d)) throw new BusinessException("CONFLICT","error.conflict",HttpStatus.CONFLICT);
            return saved;
        }
        if(!"demo-success".equals(request.paymentToken())) throw new BusinessException("PAYMENT_FAILED","error.payment_failed",HttpStatus.BAD_REQUEST);
        DocumentType type=d.type().equals("statement")?DocumentType.ACCOUNT_STATEMENT:DocumentType.EMBASSY_CERTIFICATE;
        Language language=Language.valueOf(d.language().toUpperCase(Locale.ROOT));
        var created=orderService.createDraft(new CreateOrderRequest(type,language));
        Long orderId=created.orderId();
        orderService.updateOrder(orderId,new UpdateOrderRequest(d.destination().equals("embassy")?PresentationData.EMBASSIES.get(d.embassy()):null,language));
        orderService.linkIdentity(orderId,customerId,customerId);
        var items=d.accounts().stream().map(id->{
            var detail=d.details().get(id);
            long accountId=accountId(id,customerId);
            // Custom dates remain exact in the presentation extension; upstream supports preset periods only.
            Period period=switch(detail.period()) {case "3"->Period.THREE_MONTHS;case "6"->Period.SIX_MONTHS;case "12"->Period.ONE_YEAR;default->Period.ONE_MONTH;};
            StatementType operation=switch(detail.operation()){case "income"->StatementType.INCOME;case "expense"->StatementType.EXPENSE;default->StatementType.ALL;};
            return new OrderItemRequest(accountId,Language.valueOf(detail.language().toUpperCase(Locale.ROOT)),period,operation,detail.equivalent());
        }).toList();
        orderService.addItems(orderId,new AddOrderItemsRequest(items),customerId);
        // Presentation token represents the existing test-card form, never real PAN/CVV.
        // Use the first card of the ordered accounts, otherwise any card the customer owns.
        var cards = items.stream().map(OrderItemRequest::accountId).distinct()
                .map(id -> customerService.accountsByIds(customerId, List.of(id)).stream().findFirst().orElse(null))
                .filter(Objects::nonNull)
                .flatMap(a -> a.cards().stream()).toList();
        var owned = cards.isEmpty()
                ? customerService.accountsFor(customerId, customerId).stream().flatMap(a -> a.cards().stream()).toList()
                : cards;
        long cardId = owned.stream().findFirst().orElseThrow(() ->
                new BusinessException("VALIDATION_ERROR", "error.validation", HttpStatus.BAD_REQUEST)).id();
        customerService.validateCardForCustomer(cardId, customerId);
        paymentService.pay(orderId, new PayRequest(cardId, "123"), customerId);
        // The demo has no bank operator, so the bank step runs inline: approve -> sign -> deliver.
        var advanced=orderService.advanceToDelivered(orderId);
        OrderView result=new OrderView(created.orderNumber(),orderId,customer.fullName(),d.type(),d.embassy(),d.destination(),d.recipient(),d.language(),List.copyOf(d.accounts()),Map.copyOf(d.details()),true,Instant.now().toString(),type.getPrice().intValueExact(),"pending","paid",false,advanced.status());
        PresentationOrder entity=new PresentationOrder();entity.requestKey=request.idempotencyKey();entity.customerId=customerId;entity.orderId=orderId;entity.orderNumber=result.id();entity.status="pending";entity.payload=json.writeValueAsString(result);repository.save(entity);
        return result;
    }
    private long accountId(String productId, Long customerId) {
        var presentation=PresentationData.PRODUCTS.stream().filter(p->p.id().equals(productId)).findFirst();
        if(presentation.isPresent()) {
            long accountId=presentation.get().accountId();
            customerService.validateAccountForCustomer(accountId,customerId);
            return accountId;
        }
        long id=Long.parseLong(productId.substring(productId.indexOf('-')+1));
        if(productId.startsWith("card-")) {
            // Presentation fixtures map card-N to account-N (cardId == accountId in seed). Validate ownership via account.
            long accountIdForCard = id;
            customerService.validateAccountForCustomer(accountIdForCard, customerId);
            return accountIdForCard;
        }
        customerService.validateAccountForCustomer(id,customerId);
        return id;
    }
    private OrderView view(PresentationOrder entity) {
        OrderView v=json.readValue(entity.payload,OrderView.class);
        String realStatus=orders.findById(entity.orderId).map(order->order.getStatus().name()).orElse(v.realStatus());
        return new OrderView(v.id(),entity.orderId,v.customer(),v.type(),v.embassy(),v.destination(),v.recipient(),v.language(),v.accounts(),v.details(),v.reviewed(),v.date(),v.price(),entity.status,v.paymentStatus(),false,realStatus);
    }
    @Transactional(readOnly=true) public List<OrderView> list() { return repository.findAllByOrderByIdDesc().stream().map(this::view).toList(); }
    @Transactional public OrderView status(String number, String status) {
        if(!Set.of("pending","completed","rejected").contains(Objects.toString(status,""))) throw invalid();
        var entity=repository.findByOrderNumber(number).orElseThrow(()->new BusinessException("ORDER_NOT_FOUND","error.order_not_found",HttpStatus.NOT_FOUND));
        if(!view(entity).destination().equals("embassy")) throw invalid();
        if(entity.status.equals(status)) return view(entity);
        var order=orders.findById(entity.orderId).orElseThrow();
        if(!status.equals("pending")) {
            OrderStatus target=status.equals("completed")?OrderStatus.COMPLETED:OrderStatus.REJECTED;
            // The real portal can also act on a presentation order, so only drive the transition
            // when the order is still awaiting the embassy; otherwise just reflect the real state.
            if(order.getStatus()!=target) {
                // Go through the real portal flow so the demo exercises the same guards as production:
                // the embassy may only complete/reject an order that is already DELIVERED.
                Long portalUserId=portalUserService.findActiveIdByEmbassyId(order.getEmbassyId());
                if(portalUserId==null) throw new BusinessException("UNAUTHORIZED","error.unauthorized",HttpStatus.UNAUTHORIZED);
                var request=new UpdateDocumentStatusRequest(
                        status.equals("completed")?PortalDocumentStatus.COMPLETED:PortalDocumentStatus.REJECTED,
                        status.equals("rejected")?"Demo tərəfindən rədd edildi":null);
                portalService.updateStatus(number,request,portalUserId);
            }
        }
        entity.status=status;
        return view(entity);
    }
}
