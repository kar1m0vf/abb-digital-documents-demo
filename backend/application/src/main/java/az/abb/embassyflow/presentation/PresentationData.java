package az.abb.embassyflow.presentation;

import java.math.BigDecimal;
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
            jdbc.update("INSERT INTO portal_users(id,embassy_id,username,password,full_name,role,active,created_at,updated_at) VALUES(1,101,'admin@italy','demo1234','Aydan Ahadova','ADMIN',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
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
}
