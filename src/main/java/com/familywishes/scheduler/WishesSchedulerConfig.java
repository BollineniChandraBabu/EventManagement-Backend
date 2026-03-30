package com.familywishes.scheduler;

import java.util.Arrays;
import java.util.TimeZone;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WishesSchedulerConfig {
  @Value("${app.chat.unread-mail.cron:0 0 2 * * ?}")
  private String unreadChatMailCron;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Bean
  JobDetail wishesJobDetail() {
    return JobBuilder.newJob(WishesScheduler.class)
        .withIdentity("dailyWishJob")
        .storeDurably()
        .build();
  }

  @Bean
  Trigger wishesTrigger(JobDetail wishesJobDetail) {
    return TriggerBuilder.newTrigger()
        .forJob(wishesJobDetail)
        .withIdentity("dailyWishTrigger")
        .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(6, 10))
        .build();
  }

  @Bean
  public JobDetail morningJobDetail() {

    return JobBuilder.newJob(GoodMorningScheduler.class)
        .withIdentity("goodMorningJob")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger morningTrigger() {

    return TriggerBuilder.newTrigger()
        .forJob(morningJobDetail())
        .withIdentity("goodMorningTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 0 6 * * ?")
                .inTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }

  @Bean
  public JobDetail nightJobDetail() {

    return JobBuilder.newJob(GoodNightScheduler.class)
        .withIdentity("goodNightJob")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger nightTrigger() {

    return TriggerBuilder.newTrigger()
        .forJob(nightJobDetail())
        .withIdentity("goodNightTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 0 22 * * ?")
                .inTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }

  @Bean
  public JobDetail birthdayJobDetail() {

    return JobBuilder.newJob(BirthdayMailScheduler.class)
        .withIdentity("birthdayJob")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger birthdayTrigger() {

    return TriggerBuilder.newTrigger()
        .forJob(birthdayJobDetail())
        .withIdentity("birthdayTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 30 6 * * ?")
                .inTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }

  @Bean
  public JobDetail refreshTokenCleanupJobDetail() {
    return JobBuilder.newJob(RefreshTokenCleanupScheduler.class)
        .withIdentity("refreshTokenCleanupJob")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger refreshTokenCleanupTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(refreshTokenCleanupJobDetail())
        .withIdentity("refreshTokenCleanupTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 0 0 * * ?")
                .inTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }

  @Bean
  public JobDetail festivalWishJobDetail() {
    return JobBuilder.newJob(FestivalScheduler.class)
        .withIdentity("festivalWishScheduler")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger festivalWishTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(festivalWishJobDetail())
        .withIdentity("festivalWishTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 30 7 * * ?")
                .inTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }

  @Bean
  public JobDetail festivalSyncJobDetail() {
    return JobBuilder.newJob(FestivalSyncScheduler.class)
        .withIdentity("festivalMonthlySyncScheduler")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger festivalSyncTrigger() {
    return TriggerBuilder.newTrigger()
        .forJob(festivalSyncJobDetail())
        .withIdentity("festivalMonthlySyncTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 0 2 1 * ?")
                .inTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))
                .withMisfireHandlingInstructionFireAndProceed())
        .build();
  }

  @Bean
  public JobDetail unreadChatEmailJobDetail() {
    return JobBuilder.newJob(com.familywishes.chat.UnreadChatEmailScheduler.class)
        .withIdentity("unreadChatEmailJob")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger unreadChatEmailTrigger() {
    String cronExpression = toValidQuartzCron(unreadChatMailCron);
    return TriggerBuilder.newTrigger()
        .forJob(unreadChatEmailJobDetail())
        .withIdentity("unreadChatEmailTrigger")
        .withSchedule(
            CronScheduleBuilder.cronSchedule(cronExpression)
                .inTimeZone(TimeZone.getTimeZone(schedulerTimeZone))
                .withMisfireHandlingInstructionDoNothing())
        .build();
  }

  private String toValidQuartzCron(String cron) {
    if (CronExpression.isValidExpression(cron)) {
      return cron;
    }

    String[] fields = cron == null ? new String[0] : cron.trim().split("\\s+");
    if (fields.length == 6 && "*".equals(fields[3]) && "*".equals(fields[5])) {
      String patched =
          String.join(
              " ",
              Arrays.asList(fields[0], fields[1], fields[2], fields[3], fields[4], "?"));
      if (CronExpression.isValidExpression(patched)) {
        return patched;
      }
    }

    throw new IllegalArgumentException(
        "Invalid Quartz cron expression for app.chat.unread-mail.cron: " + cron);
  }
}
