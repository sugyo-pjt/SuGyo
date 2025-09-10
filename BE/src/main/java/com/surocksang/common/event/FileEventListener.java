package com.surocksang.common.event;

import com.surocksang.common.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventListener {

    private final FileStorageService fileStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleFileRollback(FileRollbackEvent event){
        String fileUrl = event.getFileUrl();
        log.debug("transaction rolled back. Deleting file: " + fileUrl);
        try{
            fileStorageService.deleteFile(fileUrl);
        } catch (Exception e){
            // 트랜잭션 롤백은 됐으니까 일단 확인만 하기
            log.info("Failed to delete file after transaction rollback: " + fileUrl, e);
        }
    }
}
