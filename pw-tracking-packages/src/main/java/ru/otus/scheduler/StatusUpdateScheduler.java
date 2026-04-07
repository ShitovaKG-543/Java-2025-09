package ru.otus.scheduler;

import ru.otus.db.DatabaseManager;
import ru.otus.service.TrackingService;
import org.quartz.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.sql.*;

public class StatusUpdateScheduler {
    private final TrackingService trackingService;
    private final AbsSender bot;

    public StatusUpdateScheduler(TrackingService trackingService, AbsSender bot) {
        this.trackingService = trackingService;
        this.bot = bot;
    }

    public void start(String cronExpression) throws SchedulerException {
        SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();

        JobDetail job = JobBuilder.newJob(UpdateJob.class)
                .withIdentity("statusUpdate", "group1")
                .build();

        job.getJobDataMap().put("trackingService", trackingService);
        job.getJobDataMap().put("bot", bot);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "group1")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        scheduler.scheduleJob(job, trigger);
        scheduler.start();
    }

    public static class UpdateJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            TrackingService trackingService = (TrackingService) context.getJobDetail().getJobDataMap().get("trackingService");
            AbsSender bot = (AbsSender) context.getJobDetail().getJobDataMap().get("bot");

            // Получаем все уникальные посылки из БД
            String sql = "SELECT DISTINCT user_id, tracking_number, service_name, last_status FROM parcels";

            try (Connection conn = DatabaseManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Long userId = rs.getLong("user_id");
                    String trackingNumber = rs.getString("tracking_number");
                    String oldStatus = rs.getString("last_status");

                    // Получаем новый статус (кэш пропускаем - принудительное обновление)
                    String newStatus = trackingService.getTrackingStatus(userId, trackingNumber);

                    if (!newStatus.equals(oldStatus) && oldStatus != null) {
                        // Отправляем уведомление пользователю
                        SendMessage message = new SendMessage();
                        message.setChatId(userId.toString());
                        message.setText("📦 Изменение статуса посылки " + trackingNumber + ":\n" +
                                "Было: " + oldStatus + "\n" +
                                "Стало: " + newStatus);
                        bot.execute(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
