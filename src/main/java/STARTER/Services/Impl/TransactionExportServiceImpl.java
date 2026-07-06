package STARTER.Services.Impl;

import STARTER.DTOs.TransactionHistoryFilter;
import STARTER.DTOs.TransactionViewDTO;
import STARTER.Enums.AccountStatus;
import STARTER.Services.Interface.TransactionExportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
// Advanced — generates PDF downloads (not email)
public class TransactionExportServiceImpl implements TransactionExportService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionExportServiceImpl.class);

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] HEADERS = {
            "Type", "Status", "Amount", "Spending Category", "Date", "From", "To", "Account Status"
    };

    @Override
    public void exportPdf(List<TransactionViewDTO> transactions, TransactionHistoryFilter filter, String username, HttpServletResponse response

    ) throws IOException {
        String filename = "aswallet-transactions-" + LocalDate.now().format(FILE_DATE) + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(document, response.getOutputStream());

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        document.open();

        Paragraph title = new Paragraph("ASWallet Transaction Report", titleFont);

        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8f);
        document.add(title);

        document.add(new Paragraph("User: " + username, metaFont));
        document.add(new Paragraph("Filters: " + describeFilters(filter), metaFont));
        document.add(new Paragraph("Generated at: " + LocalDateTime.now().format(GENERATED_AT), metaFont));
        document.add(new Paragraph(" ", metaFont));

        PdfPTable table = new PdfPTable(HEADERS.length);

        table.setWidthPercentage(100f);
        table.setSpacingBefore(6f);

        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));

            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5f);
            table.addCell(cell);
        }

        for (TransactionViewDTO transaction : transactions) {

            for (String value : toRowValues(transaction)) {
                PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
                cell.setPadding(4f);
                table.addCell(cell);
            }
        }

        document.add(table);
        document.close();
        logger.info("Transaction PDF exported: username={}, rowCount={}, filename={}",
                username, transactions.size(), filename);
    }

    private String[] toRowValues(TransactionViewDTO transaction) {
        return new String[] {

                safe(transaction.getType() != null ? transaction.getType().name() : null),
                safe(transaction.getStatus() != null ? transaction.getStatus().name() : null),
                transaction.getAmount() != null ? transaction.getAmount().toPlainString() : "-",
                safe(transaction.getDescription()),
                safe(transaction.getCreatedAt()),
                maskUsername(transaction.getSenderUsername()),
                maskUsername(transaction.getReceiverUsername()),
                formatAccountStatus(transaction)
        };
    }

    private String formatAccountStatus(TransactionViewDTO transaction) {
        StringBuilder builder = new StringBuilder();

        if (transaction.getSenderUsername() != null) {
            builder.append("From: ").append(formatStatus(transaction.getSenderAccountStatus()));
        }

        if (transaction.getReceiverUsername() != null) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }

            builder.append("To: ").append(formatStatus(transaction.getReceiverAccountStatus()));
        }

        return builder.isEmpty() ? "-" : builder.toString();
    }

    private String formatStatus(AccountStatus status) {
        return status != null
                ? status.name()
                : "-";
    }

    private String maskUsername(String username) {
        return username != null
                ? username
                : "****";
    }

    private String safe(String value) {
        return value != null && !value.isBlank()
                ? value
                : "-";
    }

    private String describeFilters(TransactionHistoryFilter filter) {
        if (filter == null || !filter.hasActiveFilters()) {
            return "All transactions";
        }

        return Stream.of(
                        filter.getType() != null ? "Type=" + filter.getType() : null,
                        filter.getStatus() != null ? "Status=" + filter.getStatus() : null,
                        filter.getDateFrom() != null ? "From=" + filter.getDateFrom() : null,
                        filter.getDateTo() != null ? "To=" + filter.getDateTo() : null,
                        filter.getSpendingCategory() != null ? "Category=" + filter.getSpendingCategory().getLabel() : null,
                        filter.getAmount() != null ? "Amount=" + filter.getAmount() : null
                )
                .filter(Objects::nonNull) // .filter(value -> value != null
                .collect(Collectors.joining(", "));
    }
}
