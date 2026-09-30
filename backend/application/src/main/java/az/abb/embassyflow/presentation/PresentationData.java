package az.abb.embassyflow.presentation;

import az.abb.embassyflow.order.enums.DocumentType;
import az.abb.embassyflow.order.enums.OrderStatus;
import az.abb.embassyflow.order.enums.TimelineStep;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Fixtures match the upstream demo data and the client-approved presentation. */
@Component
@Profile("presentation")
public class PresentationData implements ApplicationRunner {
    public static final long CUSTOMER_ID = 101;
    public static final List<String> FIN_CODES = List.of(
            "5D7X9Q2", "6F8A1B3", "2K9C4D7", "7H2E5F1", "3J6T8G4",
            "8K4P6S2", "9M2R7T4", "4N6Q9W1", "1L5H3J8", "7Z2C5V9");

    public record Product(String id, long accountId, String currency, String last4, String balance, boolean card) {}
    private record CustomerFixture(long id, String fin, String name, String phone) {}
    private record AccountFixture(long id, long customerId, String number, String currency, String balance, String type) {}
    private record CardFixture(long id, long accountId, String number, String brand, String expiry) {}
    private record PortalUserFixture(long id, long embassyId, String username, String password, String fullName, String role) {}
    private record SeedOrder(String orderNumber, DocumentType documentType, String language, OrderStatus status,
                             long embassyId, long customerId, long accountId, String createdAt, List<TimelineStep> steps) {}

    public static final List<Product> PRODUCTS = List.of(
        new Product("visa-azn", 101, "AZN", "7575", "2450.80", true),
        new Product("master-azn", 102, "AZN", "4581", "680.25", true),
        new Product("visa-usd", 103, "USD", "9032", "1200.00", true),
        new Product("credit", 104, "AZN", "1084", "-350.00", true),
        new Product("account-azn", 105, "AZN", "2156", "5230.50", false),
        new Product("account-usd", 106, "USD", "3860", "3400.00", false),
        new Product("account-eur", 107, "EUR", "6241", "1850.00", false));

    private static final List<CustomerFixture> UPSTREAM_CUSTOMERS = List.of(
        new CustomerFixture(1, "5D7X9Q2", "Aydan Ahadova", "+994503304582"),
        new CustomerFixture(2, "6F8A1B3", "Elvin M\u0259mm\u0259dov", "+994553216780"),
        new CustomerFixture(3, "2K9C4D7", "Nigar \u018fliyeva", "+994706658214"),
        new CustomerFixture(4, "7H2E5F1", "R\u0259\u015fad Quliyev", "+994507788341"),
        new CustomerFixture(5, "3J6T8G4", "Leyla H\u00fcseynova", "+994551234950"),
        new CustomerFixture(6, "8K4P6S2", "Tural H\u00fcseynov", "+994502228811"),
        new CustomerFixture(7, "9M2R7T4", "Samir N\u0259sirov", "+994505551122"),
        new CustomerFixture(8, "4N6Q9W1", "Fidan Abbasova", "+994553334455"),
        new CustomerFixture(9, "1L5H3J8", "Orxan Q\u0259hr\u0259manov", "+994707776677"),
        new CustomerFixture(10, "7Z2C5V9", "G\u00fcnay M\u0259mm\u0259dli", "+994518889900"));

    private static final List<AccountFixture> UPSTREAM_ACCOUNTS = List.of(
        new AccountFixture(1, 1, "19473526745367352156", "AZN", "12500.50", "CURRENT"),
        new AccountFixture(2, 1, "19473526745367352157", "USD", "3400.00", "CURRENT"),
        new AccountFixture(3, 2, "19473526745367352158", "AZN", "820.75", "SAVING"),
        new AccountFixture(4, 3, "19473526745367352159", "EUR", "2150.00", "CURRENT"),
        new AccountFixture(5, 4, "19473526745367352160", "AZN", "-300.25", "CURRENT"),
        new AccountFixture(6, 5, "19473526745367352161", "AZN", "9999.99", "SAVING"),
        new AccountFixture(8, 6, "19473526745367352163", "AZN", "4520.10", "SAVING"),
        new AccountFixture(9, 7, "19473526745367352164", "AZN", "3100.00", "CURRENT"),
        new AccountFixture(10, 8, "19473526745367352165", "AZN", "7800.55", "CURRENT"),
        new AccountFixture(11, 9, "19473526745367352166", "AZN", "990.25", "CURRENT"),
        new AccountFixture(12, 10, "19473526745367352167", "AZN", "6050.40", "SAVING"));

    private static final List<CardFixture> UPSTREAM_CARDS = List.of(
        new CardFixture(1, 1, "7812 **** **** 4581", "VISA", "05/26"),
        new CardFixture(2, 2, "5412 **** **** 7788", "MASTERCARD", "09/25"),
        new CardFixture(3, 3, "4169 **** **** 3321", "VISA", "11/26"),
        new CardFixture(4, 4, "5525 **** **** 9014", "MASTERCARD", "03/26"),
        new CardFixture(5, 5, "4021 **** **** 6642", "VISA", "07/25"),
        new CardFixture(6, 6, "4169 **** **** 4950", "VISA", "12/30"),
        // The second upstream seed adds accounts without cards. Local demo cards keep payment usable.
        new CardFixture(7, 8, "4169 **** **** 2163", "VISA", "12/30"),
        new CardFixture(8, 9, "4169 **** **** 2164", "VISA", "12/30"),
        new CardFixture(9, 10, "4169 **** **** 2165", "VISA", "12/30"),
        new CardFixture(10, 11, "4169 **** **** 2166", "VISA", "12/30"),
        new CardFixture(11, 12, "4169 **** **** 2167", "VISA", "12/30"));

    public static final Map<String, Long> EMBASSIES = Map.of(
            "italy",101L,"france",102L,"usa",103L,"germany",104L,"spain",105L,"uk",106L);

    private static final List<PortalUserFixture> PORTAL_USERS = List.of(
            new PortalUserFixture(1, 101L, "admin@italy", "demo1234", "Aydan Ahadova", "ADMIN"),
            new PortalUserFixture(2, 102L, "admin@france", "demo1234", "Marie Dubois", "ADMIN"),
            new PortalUserFixture(3, 103L, "admin@usa", "demo1234", "John Carter", "ADMIN"),
            new PortalUserFixture(4, 104L, "admin@germany", "demo1234", "Anna Schmidt", "ADMIN"),
            new PortalUserFixture(5, 105L, "admin@spain", "demo1234", "Carlos Ruiz", "ADMIN"),
            new PortalUserFixture(6, 106L, "admin@uk", "demo1234", "Emma Wilson", "ADMIN"));

    // Ascending document number: the portal lists newest first by id.
    private static final List<SeedOrder> SEED_ORDERS = List.of(
            seed("AR-2026-000087", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.PAYMENT_RECEIVED, 102L, 10L, 12L, "2026-09-03 08:30:00", false),
            seed("AR-2026-000091", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.REJECTED, 102L, 9L, 11L, "2026-09-05 15:10:00", true),
            seed("AR-2026-000098", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.DELIVERED, 101L, 8L, 10L, "2026-09-08 10:45:00", true),
            seed("AR-2026-000103", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.PAYMENT_RECEIVED, 101L, 7L, 9L, "2026-09-10 13:25:00", false),
            seed("AR-2026-000118", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.DELIVERED, 101L, 5L, 6L, "2026-09-12 09:15:00", true),
            seed("AR-2026-000124", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.REJECTED, 101L, 4L, 5L, "2026-09-14 16:40:00", true),
            seed("AR-2026-000125", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.DELIVERED, 101L, 3L, 4L, "2026-09-15 11:05:00", true),
            seed("AR-2026-000471", DocumentType.ACCOUNT_STATEMENT, "EN", OrderStatus.PAYMENT_RECEIVED, 101L, 6L, 8L, "2026-09-16 14:20:00", false),
            seed("AR-2026-000489", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.PAYMENT_RECEIVED, 101L, 2L, 3L, "2026-09-17 09:30:00", false),
            seed("AR-2026-000512", DocumentType.ACCOUNT_STATEMENT, "AZ", OrderStatus.DELIVERED, 101L, 1L, 1L, "2026-09-18 10:00:00", true));

    private static SeedOrder seed(String orderNumber, DocumentType documentType, String language,
                                  OrderStatus status, long embassyId, long customerId, long accountId,
                                  String createdAt, boolean delivered) {
        List<TimelineStep> steps = delivered
                ? List.of(TimelineStep.ORDER_RECEIVED, TimelineStep.OTP_VERIFIED, TimelineStep.PAYMENT_RECEIVED,
                        TimelineStep.ABB_APPROVED, TimelineStep.DIGITALLY_SIGNED, TimelineStep.DELIVERED_TO_EMBASSY)
                : List.of(TimelineStep.ORDER_RECEIVED, TimelineStep.OTP_VERIFIED, TimelineStep.PAYMENT_RECEIVED);
        return new SeedOrder(orderNumber, documentType, language, status, embassyId, customerId, accountId, createdAt, steps);
    }

    private final JdbcTemplate jdbc;
    public PresentationData(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void run(ApplicationArguments args) {
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS order_no_seq START WITH 1");
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS txn_no_seq START WITH 1");

        for (var customer : UPSTREAM_CUSTOMERS) seedCustomer(customer);
        for (var account : UPSTREAM_ACCOUNTS) seedAccount(account);
        for (var card : UPSTREAM_CARDS) seedCard(card);

        seedCustomer(new CustomerFixture(CUSTOMER_ID, "ABC1234", "Aydan \u018fh\u0259dova", "+994500000000"));
        for (var product : PRODUCTS) {
            seedAccount(new AccountFixture(product.accountId(), CUSTOMER_ID,
                    "AZ00000000000000000000" + product.last4(), product.currency(), product.balance(), "CURRENT"));
            if (product.card()) {
                seedCard(new CardFixture(product.accountId(), product.accountId(), "**** **** **** " + product.last4(),
                        product.id().startsWith("master") ? "MASTERCARD" : "VISA", "12/30"));
            }
        }

        for (var entry : EMBASSIES.entrySet()) {
            if (count("embassies", entry.getValue()) == 0) {
                jdbc.update("INSERT INTO embassies(id,name,country,city,language,active,created_at,updated_at) VALUES(?,?,?,?,?,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                        entry.getValue(), entry.getKey(), entry.getKey(), "Bak\u0131", "EN");
            }
        }
        if (count("portal_users", 1) == 0) {
            // One portal user per presentation embassy so the demo can drive the real
            // PortalService.updateStatus path instead of writing order rows directly.
            for (var user : PORTAL_USERS) {
                if (count("portal_users", user.id()) == 0) {
                    jdbc.update("INSERT INTO portal_users(id,embassy_id,username,password,full_name,role,active,created_at,updated_at) VALUES(?,?,?,?,?,?,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                            user.id(), user.embassyId(), user.username(),
                            user.password(), user.fullName(), user.role());
                }
            }
        }

        // Keep generated order numbers clear of the seeded range (order_number is unique).
        jdbc.execute("ALTER SEQUENCE order_no_seq RESTART WITH 6001");
        for (var order : SEED_ORDERS) {
            if (countBy("document_orders", "order_number", order.orderNumber()) != 0) {
                continue;
            }
            BigDecimal price = order.documentType().getPrice();
            // No explicit id: the sequence keeps the seeded rows older than later server orders.
            jdbc.update("INSERT INTO document_orders(document_type,language,status,order_number,embassy_id,customer_id,version,created_at,updated_at) VALUES(?,?,?,?,?,?,0,?,?)",
                    order.documentType().name(), order.language(), order.status().name(), order.orderNumber(),
                    order.embassyId(), order.customerId(), Timestamp.valueOf(order.createdAt()), Timestamp.valueOf(order.createdAt()));
            long orderId = jdbc.queryForObject("SELECT id FROM document_orders WHERE order_number = ?",
                    Long.class, order.orderNumber());
            jdbc.update("INSERT INTO order_items(order_id,account_id,language,period,statement_type,equivalent_currency,created_at,updated_at) VALUES(?,?,?,'3M','ALL',FALSE,?,?)",
                    orderId, order.accountId(), order.language(), Timestamp.valueOf(order.createdAt()), Timestamp.valueOf(order.createdAt()));
            for (var step : order.steps()) {
                jdbc.update("INSERT INTO order_timeline(order_id,step,created_at,updated_at) VALUES(?,?,?,?)",
                        orderId, step.name(), Timestamp.valueOf(order.createdAt()), Timestamp.valueOf(order.createdAt()));
            }
            Long cardId = jdbc.queryForObject("SELECT id FROM cards WHERE account_id = ? ORDER BY id LIMIT 1",
                    Long.class, order.accountId());
            jdbc.update("INSERT INTO payments(order_id,card_id,amount,currency,status,transaction_no,created_at,updated_at) VALUES(?,?,?,?,'SUCCESS',?,?,?)",
                    orderId, cardId, price, order.documentType().getCurrency(), "TXN-SEED-" + order.orderNumber(),
                    Timestamp.valueOf(order.createdAt()), Timestamp.valueOf(order.createdAt()));
        }
    }

    private void seedCustomer(CustomerFixture value) {
        if (count("customers", value.id()) == 0) {
            jdbc.update("INSERT INTO customers(id,fin,full_name,phone,created_at,updated_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    value.id(), value.fin(), value.name(), value.phone());
        }
    }

    private void seedAccount(AccountFixture value) {
        if (count("accounts", value.id()) == 0) {
            jdbc.update("INSERT INTO accounts(id,customer_id,account_number,currency,balance,account_type,active,created_at,updated_at) VALUES(?,?,?,?,?,?,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    value.id(), value.customerId(), value.number(), value.currency(), new BigDecimal(value.balance()), value.type());
        }
    }

    private void seedCard(CardFixture value) {
        if (count("cards", value.id()) == 0) {
            jdbc.update("INSERT INTO cards(id,account_id,masked_number,card_brand,expiry,active,created_at,updated_at) VALUES(?,?,?,?,?,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    value.id(), value.accountId(), value.number(), value.brand(), value.expiry());
        }
    }

    private int count(String table, long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
    }

    private int countBy(String table, String column, String value) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
    }
}
