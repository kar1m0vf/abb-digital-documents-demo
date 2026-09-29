package az.abb.embassyflow.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import az.abb.embassyflow.common.exception.BusinessException;
import az.abb.embassyflow.common.exception.ErrorCodes;
import az.abb.embassyflow.customer.dao.entity.Account;
import az.abb.embassyflow.customer.dao.entity.Card;
import az.abb.embassyflow.customer.dao.repository.AccountRepository;
import az.abb.embassyflow.customer.dao.repository.CardRepository;
import az.abb.embassyflow.customer.dao.repository.CustomerRepository;
import az.abb.embassyflow.customer.dto.response.AccountResponse;
import az.abb.embassyflow.customer.enums.AccountType;
import az.abb.embassyflow.customer.enums.Currency;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void accountsFor_validToken_returnsAccountsAndFiltersInactiveCards() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        Account account = account(new BigDecimal("12500.50"), AccountType.CURRENT);
        account.setCards(List.of(
                card("7812 **** **** 4581", true),
                card("7812 **** **** 0000", false)));
        when(accountRepository.findByCustomerIdAndActiveTrueOrderById(1L)).thenReturn(List.of(account));

        List<AccountResponse> result = customerService.accountsFor(1L, 1L);

        assertEquals(1, result.size());
        AccountResponse response = result.get(0);
        assertEquals("19473526745367352156", response.accountNumber());
        assertEquals(Currency.AZN, response.currency());
        assertEquals(new BigDecimal("12500.50"), response.balance());
        assertEquals(AccountType.CURRENT, response.type());
        assertEquals(1, response.cards().size());
        assertEquals("7812 **** **** 4581", response.cards().get(0).maskedNumber());
        assertEquals("VISA", response.cards().get(0).brand());
    }

    @Test
    void accountsFor_missingToken_throwsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerService.accountsFor(1L, null));

        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void accountsFor_tokenMismatch_throwsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerService.accountsFor(1L, 2L));

        assertEquals(ErrorCodes.UNAUTHORIZED, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void accountsFor_unknownCustomer_throwsCustomerNotFound() {
        when(customerRepository.existsById(1L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerService.accountsFor(1L, 1L));

        assertEquals(ErrorCodes.CUSTOMER_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void validateCardForCustomer_unknownCard_throwsCardNotFound() {
        when(cardRepository.existsByIdAndAccount_CustomerIdAndActiveTrue(21L, 1L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerService.validateCardForCustomer(21L, 1L));

        assertEquals(ErrorCodes.CARD_NOT_FOUND, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void validateCardForCustomer_validCard_doesNotThrow() {
        when(cardRepository.existsByIdAndAccount_CustomerIdAndActiveTrue(21L, 1L)).thenReturn(true);

        customerService.validateCardForCustomer(21L, 1L);
    }

    private Account account(BigDecimal balance, AccountType type) {
        Account account = new Account();
        account.setAccountNumber("19473526745367352156");
        account.setCurrency(Currency.AZN);
        account.setBalance(balance);
        account.setAccountType(type);
        return account;
    }

    private Card card(String maskedNumber, boolean active) {
        Card card = new Card();
        card.setMaskedNumber(maskedNumber);
        card.setCardBrand("VISA");
        card.setExpiry("05/26");
        card.setActive(active);
        return card;
    }
}