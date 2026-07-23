package STARTER.Services.Interface;

import STARTER.DTOs.TransactionHistoryFilter;
import STARTER.DTOs.TransactionViewDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface TransactionExportService {

    void exportPdf(
            List<TransactionViewDTO> transactions,
            TransactionHistoryFilter filter,
            String username,
            HttpServletResponse response) throws IOException;
}
