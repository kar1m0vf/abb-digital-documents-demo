package az.abb.embassyflow.order.dao.entity;

import az.abb.embassyflow.common.dao.entity.AuditableEntity;
import az.abb.embassyflow.order.dao.converter.PeriodConverter;
import az.abb.embassyflow.order.enums.Language;
import az.abb.embassyflow.order.enums.Period;
import az.abb.embassyflow.order.enums.StatementType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private DocumentOrder order;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 10)
    private Language language;

    @Convert(converter = PeriodConverter.class)
    @Column(name = "period", nullable = false, length = 10)
    private Period period;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 20)
    private StatementType statementType;

    @Column(name = "equivalent_currency", nullable = false)
    private boolean equivalentCurrency;

    public Long getId() {
        return id;
    }

    public DocumentOrder getOrder() {
        return order;
    }

    public void setOrder(DocumentOrder order) {
        this.order = order;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public void setStatementType(StatementType statementType) {
        this.statementType = statementType;
    }

    public boolean isEquivalentCurrency() {
        return equivalentCurrency;
    }

    public void setEquivalentCurrency(boolean equivalentCurrency) {
        this.equivalentCurrency = equivalentCurrency;
    }
}
