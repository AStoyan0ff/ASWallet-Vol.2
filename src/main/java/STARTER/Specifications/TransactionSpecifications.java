package STARTER.Specifications;

import STARTER.DTOs.TransactionHistoryFilter;
import STARTER.Enums.SpendingCategory;
import STARTER.Models.Transaction;
import STARTER.Models.Wallet;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class TransactionSpecifications {

    private TransactionSpecifications() {}

    public static Specification<Transaction> forUserWallet(Wallet wallet, TransactionHistoryFilter filter) {

        List<Specification<Transaction>> parts = new ArrayList<>();
        parts.add(belongsToWallet(wallet));

        addIfPresent(parts, equalIfPresent("type", filter.getType()));
        addIfPresent(parts, equalIfPresent("status", filter.getStatus()));
        addIfPresent(parts, createdOnOrAfter(filter.getDateFrom()));
        addIfPresent(parts, createdOnOrBefore(filter.getDateTo()));
        addIfPresent(parts, matchesSpendingCategory(filter.getSpendingCategory()));
        addIfPresent(parts, matchesAmount(filter.getAmount()));

        return Specification.allOf(parts);
    }

    private static void addIfPresent(List<Specification<Transaction>> parts, Specification<Transaction> specification) {

        if (specification != null) {
            parts.add(specification);
        }
    }

    private static Specification<Transaction> belongsToWallet(Wallet wallet) {

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.equal(root.get("senderWallet"), wallet),
                criteriaBuilder.equal(root.get("receiverWallet"), wallet)
        );
    }

    private static Specification<Transaction> equalIfPresent(String field, Object value) {
        if (value == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value);
    }

    private static Specification<Transaction> createdOnOrAfter(LocalDate dateFrom) {
        if (dateFrom == null) {
            return null;
        }

        LocalDateTime start = dateFrom.atStartOfDay();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start);
    }

    private static Specification<Transaction> createdOnOrBefore(LocalDate dateTo) {
        if (dateTo == null) {
            return null;
        }

        LocalDateTime end = dateTo.atTime(LocalTime.MAX);

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), end);
    }

    private static Specification<Transaction> matchesSpendingCategory(SpendingCategory spendingCategory) {
        if (spendingCategory == null) {
            return null;
        }

        String prefix = spendingCategory.getLabel() + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get("description"), prefix);
    }

    private static Specification<Transaction> matchesAmount(BigDecimal amount) {

        if (amount == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("amount"), amount);
    }
}
