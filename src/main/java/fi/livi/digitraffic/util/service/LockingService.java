package fi.livi.digitraffic.util.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.livi.digitraffic.util.dao.LockingDao;

@Service
public class LockingService {
    private final LockingDao lockingDao;

    @Autowired
    public LockingService(final LockingDao lockingDao) {
        this.lockingDao = lockingDao;
    }

    @Transactional
    public boolean acquireLock(final String lockName, final String callerInstanceId, final int expirationSeconds) {
        return lockingDao.acquireLock(lockName, callerInstanceId, expirationSeconds);
    }

    @Transactional
    public void releaseLock(final String lockName, final String callerInstanceId) {
        lockingDao.releaseLock(lockName, callerInstanceId);
    }

}