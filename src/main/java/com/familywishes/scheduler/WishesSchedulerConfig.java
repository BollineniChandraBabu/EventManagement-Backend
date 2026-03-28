package com.familywishes.scheduler;

import java.util.TimeZone;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WishesSchedulerConfig {
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
}
