package dev.reagentic.transaction.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class TransactionUploadResponse {
    private String uploadBatchId;
    private int rowsParsed;
    private int rowsRejected;
    private List<String> rejectedRowErrors;
    private Map<String, BigDecimal> totalsByCategory;
    private BigDecimal netTotal;

    public TransactionUploadResponse() {
    }

    public TransactionUploadResponse(String uploadBatchId, int rowsParsed, int rowsRejected,
                                     List<String> rejectedRowErrors, Map<String, BigDecimal> totalsByCategory,
                                     BigDecimal netTotal) {
        this.uploadBatchId = uploadBatchId;
        this.rowsParsed = rowsParsed;
        this.rowsRejected = rowsRejected;
        this.rejectedRowErrors = rejectedRowErrors;
        this.totalsByCategory = totalsByCategory;
        this.netTotal = netTotal;
    }

    public String getUploadBatchId() {
        return uploadBatchId;
    }

    public void setUploadBatchId(String uploadBatchId) {
        this.uploadBatchId = uploadBatchId;
    }

    public int getRowsParsed() {
        return rowsParsed;
    }

    public void setRowsParsed(int rowsParsed) {
        this.rowsParsed = rowsParsed;
    }

    public int getRowsRejected() {
        return rowsRejected;
    }

    public void setRowsRejected(int rowsRejected) {
        this.rowsRejected = rowsRejected;
    }

    public List<String> getRejectedRowErrors() {
        return rejectedRowErrors;
    }

    public void setRejectedRowErrors(List<String> rejectedRowErrors) {
        this.rejectedRowErrors = rejectedRowErrors;
    }

    public Map<String, BigDecimal> getTotalsByCategory() {
        return totalsByCategory;
    }

    public void setTotalsByCategory(Map<String, BigDecimal> totalsByCategory) {
        this.totalsByCategory = totalsByCategory;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }

    public static TransactionUploadResponseBuilder builder() {
        return new TransactionUploadResponseBuilder();
    }

    public static class TransactionUploadResponseBuilder {
        private String uploadBatchId;
        private int rowsParsed;
        private int rowsRejected;
        private List<String> rejectedRowErrors;
        private Map<String, BigDecimal> totalsByCategory;
        private BigDecimal netTotal;

        public TransactionUploadResponseBuilder uploadBatchId(String uploadBatchId) {
            this.uploadBatchId = uploadBatchId;
            return this;
        }

        public TransactionUploadResponseBuilder rowsParsed(int rowsParsed) {
            this.rowsParsed = rowsParsed;
            return this;
        }

        public TransactionUploadResponseBuilder rowsRejected(int rowsRejected) {
            this.rowsRejected = rowsRejected;
            return this;
        }

        public TransactionUploadResponseBuilder rejectedRowErrors(List<String> rejectedRowErrors) {
            this.rejectedRowErrors = rejectedRowErrors;
            return this;
        }

        public TransactionUploadResponseBuilder totalsByCategory(Map<String, BigDecimal> totalsByCategory) {
            this.totalsByCategory = totalsByCategory;
            return this;
        }

        public TransactionUploadResponseBuilder netTotal(BigDecimal netTotal) {
            this.netTotal = netTotal;
            return this;
        }

        public TransactionUploadResponse build() {
            return new TransactionUploadResponse(uploadBatchId, rowsParsed, rowsRejected, rejectedRowErrors, totalsByCategory, netTotal);
        }
    }
}
