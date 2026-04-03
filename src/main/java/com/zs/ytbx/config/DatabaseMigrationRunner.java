package com.zs.ytbx.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
@Component
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureNoticeTable();
            ensureProductTemplateColumns();
            ensureProductCodeUniqueIndex();
            ensureInsuranceWorkflowColumns();
            migrateLegacyInsuranceWorkflowStatuses();
        } catch (DataAccessException ex) {
            log.warn("数据库迁移未执行，当前数据库不可用或连接信息未配置完整：{}", ex.getMessage());
        }
    }

    private void ensureNoticeTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS axx_notice (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                    title VARCHAR(64) NOT NULL COMMENT '通知标题',
                    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
                    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
                    published_at DATETIME NOT NULL COMMENT '发布时间',
                    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                    create_by BIGINT DEFAULT NULL COMMENT '创建人',
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_by BIGINT DEFAULT NULL COMMENT '更新人',
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    KEY idx_notice_sort_no (sort_no),
                    KEY idx_notice_published_at (published_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页通知公告表'
                """);
    }

    private void ensureProductTemplateColumns() {
        ensureColumnExists("axx_product", "template_file_name",
                "ALTER TABLE axx_product ADD COLUMN template_file_name VARCHAR(255) DEFAULT NULL COMMENT '模板文件名'");
        ensureColumnExists("axx_product", "template_file_path",
                "ALTER TABLE axx_product ADD COLUMN template_file_path VARCHAR(500) DEFAULT NULL COMMENT '模板文件路径'");
    }

    private void ensureProductCodeUniqueIndex() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'axx_product'
                  AND index_name = 'uk_product_code_deleted'
                """, Integer.class);

        if (count != null && count > 0) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE axx_product DROP INDEX uk_product_code");
        } catch (Exception ex) {
            log.debug("uk_product_code 不存在或已处理: {}", ex.getMessage());
        }

        jdbcTemplate.execute("ALTER TABLE axx_product ADD UNIQUE KEY uk_product_code_deleted (product_code, deleted)");
    }

    private void ensureInsuranceWorkflowColumns() {
        ensureColumnExists("axx_insurance_record", "review_comment",
                "ALTER TABLE axx_insurance_record ADD COLUMN review_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见'");
        ensureColumnExists("axx_insurance_record", "reviewer_id",
                "ALTER TABLE axx_insurance_record ADD COLUMN reviewer_id BIGINT DEFAULT NULL COMMENT '审核人ID'");
        ensureColumnExists("axx_insurance_record", "reviewer_name",
                "ALTER TABLE axx_insurance_record ADD COLUMN reviewer_name VARCHAR(64) DEFAULT NULL COMMENT '审核人姓名'");
        ensureColumnExists("axx_insurance_record", "review_time",
                "ALTER TABLE axx_insurance_record ADD COLUMN review_time DATETIME DEFAULT NULL COMMENT '审核时间'");
        ensureColumnExists("axx_insurance_record", "reject_reason",
                "ALTER TABLE axx_insurance_record ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL COMMENT '驳回原因'");
        ensureColumnExists("axx_insurance_record", "submit_time",
                "ALTER TABLE axx_insurance_record ADD COLUMN submit_time DATETIME DEFAULT NULL COMMENT '提交审核时间'");
        ensureColumnExists("axx_insurance_record", "underwriting_time",
                "ALTER TABLE axx_insurance_record ADD COLUMN underwriting_time DATETIME DEFAULT NULL COMMENT '进入承保时间'");
        ensureColumnExists("axx_insurance_record", "activate_time",
                "ALTER TABLE axx_insurance_record ADD COLUMN activate_time DATETIME DEFAULT NULL COMMENT '保单生效操作时间'");
    }

    private void migrateLegacyInsuranceWorkflowStatuses() {
        jdbcTemplate.update("UPDATE axx_insurance_record SET insurance_status = 'UNDERWRITING' WHERE insurance_status = 'PENDING'");
        jdbcTemplate.update("UPDATE axx_insurance_record SET insurance_status = 'ACTIVE' WHERE insurance_status = 'INSURED'");
        jdbcTemplate.update("UPDATE axx_insurance_record SET insurance_status = 'REVIEW_REJECTED' WHERE insurance_status = 'REJECTED'");
        jdbcTemplate.update("UPDATE axx_insurance_record SET insurance_status = 'PENDING_REVIEW' WHERE insurance_status = 'SUBMITTING'");

        jdbcTemplate.update("UPDATE axx_expense_record SET expense_status = 'UNDERWRITING' WHERE expense_status IN ('PENDING', 'PROCESSING')");
        jdbcTemplate.update("UPDATE axx_expense_record SET expense_status = 'ACTIVE' WHERE expense_status = 'COMPLETED'");

        jdbcTemplate.update("UPDATE axx_insurance_record SET submit_time = COALESCE(submit_time, create_time) WHERE insurance_status <> 'DRAFT'");
        jdbcTemplate.update("UPDATE axx_insurance_record SET review_time = COALESCE(review_time, update_time) WHERE insurance_status IN ('APPROVED', 'REVIEW_REJECTED', 'UNDERWRITING', 'ACTIVE', 'EXPIRED', 'CANCELLED')");
        jdbcTemplate.update("UPDATE axx_insurance_record SET underwriting_time = COALESCE(underwriting_time, review_time, update_time) WHERE insurance_status IN ('UNDERWRITING', 'ACTIVE', 'EXPIRED', 'CANCELLED')");
        jdbcTemplate.update("UPDATE axx_insurance_record SET activate_time = COALESCE(activate_time, TIMESTAMP(effective_date), update_time) WHERE insurance_status IN ('ACTIVE', 'EXPIRED')");
    }

    private void ensureColumnExists(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);

        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute(alterSql);
    }
}
