package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.ScheduledExportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduledExportRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledExportRunner.class);

    private final ScheduledExportService scheduledExportService;

    public ScheduledExportRunner(ScheduledExportService scheduledExportService) {
        this.scheduledExportService = scheduledExportService;
    }

    @Scheduled(cron = "0 * * * * *") // Run every minute
    public void runScheduledExports() {
        logger.info("Checking for scheduled exports to run...");
        List<ScheduledExportDto> scheduledExports = scheduledExportService.getAllScheduledExports();
        for (ScheduledExportDto scheduledExport : scheduledExports) {
            if (scheduledExport.isEnabled()) {
                logger.info("Checking schedule for '{}' with cron expression '{}'", scheduledExport.getName(), scheduledExport.getSchedule());
                if (isTimeToRun(scheduledExport)) {
                    logger.info("Running scheduled export: {}", scheduledExport.getName());
                    // In a real application, you would trigger the export process here.
                    // For now, we'll just log that the export would have run.
                } else {
                    logger.info("Skipping scheduled export '{}', not scheduled to run now.", scheduledExport.getName());
                }
            } else {
                logger.info("Skipping disabled scheduled export: {}", scheduledExport.getName());
            }
        }
    }

    private boolean isTimeToRun(ScheduledExportDto scheduledExport) {
        try {
            logger.info("Attempting to parse cron expression with Spring's CronExpression: '{}'", scheduledExport.getSchedule());
            CronExpression cronExpression = CronExpression.parse(scheduledExport.getSchedule());
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime lastExecutionTime = now.minusMinutes(1).minusSeconds(1); // Check if it ran in the last minute
            Optional<LocalDateTime> nextExecution = Optional.ofNullable(cronExpression.next(lastExecutionTime.toLocalDateTime()));

            boolean isTime = nextExecution.isPresent() && !nextExecution.get().isAfter(now.toLocalDateTime());
            logger.info("isTimeToRun for '{}': {}", scheduledExport.getName(), isTime);
            return isTime;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid cron expression for scheduled export '{}' using Spring's CronExpression: {}", scheduledExport.getName(), scheduledExport.getSchedule(), e);
            return false;
        }
    }
}
