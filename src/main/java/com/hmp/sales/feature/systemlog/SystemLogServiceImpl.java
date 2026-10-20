package com.hmp.sales.feature.systemlog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation của {@link SystemLogService}.
 * <p>
 * Hiện tại chỉ ghi ra application log (console) thay vì lưu vào DB.
 * Entity {@code SystemLog} và persistence đầy đủ sẽ được implement ở Phase 5.
 *
 * <p><strong>TODO Phase 5</strong>: Inject {@code SystemLogRepository} và persist vào PostgreSQL.
 */
@Slf4j
@Service
public class SystemLogServiceImpl implements SystemLogService {

    /**
     * {@inheritDoc}
     * <p>
     * Stub: ghi vào application log thay vì DB.
     */
    @Override
    public void log(Long actorId, String action, String entityType, Long entityId, String description) {
        // TODO Phase 5: persist SystemLog entity vào PostgreSQL
        log.info("[SystemLog] actor={} | action={} | entity={}:{} | desc={}",
                actorId, action, entityType, entityId, description);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stub: ghi vào application log thay vì DB.
     */
    @Override
    public void logWithChange(Long actorId, String action, String entityType, Long entityId,
                              String description, String oldValue, String newValue) {
        // TODO Phase 5: persist SystemLog entity (kèm oldValue/newValue) vào PostgreSQL
        log.info("[SystemLog] actor={} | action={} | entity={}:{} | desc={} | old=[{}] | new=[{}]",
                actorId, action, entityType, entityId, description, oldValue, newValue);
    }
}
