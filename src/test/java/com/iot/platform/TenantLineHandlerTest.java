package com.iot.platform;

import com.iot.platform.tenant.TenantContext;
import com.iot.platform.tenant.TenantLineHandlerImpl;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多租户隔离处理器测试
 * 验证 tenant_id 注入和表忽略逻辑
 */
class TenantLineHandlerTest {

    private TenantLineHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new TenantLineHandlerImpl();
    }

    @Test
    void testGetTenantIdWhenSet() {
        TenantContext.setTenantId(100L);
        assertNotNull(handler.getTenantId(), "设置 tenantId 后应返回非 null");
    }

    @Test
    void testGetTenantIdWhenNotSet() {
        TenantContext.clear();
        assertNotNull(handler.getTenantId(), "未设置 tenantId 时应返回 NullValue（非 null）");
    }

    @Test
    void testIgnoreTableForTenantTable() {
        assertTrue(handler.ignoreTable("tenant"), "tenant 表应被忽略");
    }

    @Test
    void testIgnoreTableForDictTable() {
        assertTrue(handler.ignoreTable("sys_dict"), "系统字典表应被忽略");
    }

    @Test
    void testDoNotIgnoreBusinessTable() {
        TenantContext.setIgnore(false);
        assertFalse(handler.ignoreTable("device"), "device 表不应被忽略");
        assertFalse(handler.ignoreTable("energy_meter"), "energy_meter 表不应被忽略");
    }

    @Test
    void testIgnoreAllTablesWhenSuperAdmin() {
        TenantContext.setIgnore(true);
        assertTrue(handler.ignoreTable("device"), "超管模式下所有表应被忽略");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
}
