package com.slatto.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	public static final String MAIL_EXECUTOR = "mailExecutor";

	// 메일 전용 풀이다. 공용 풀을 쓰면 SMTP 지연이 다른 비동기 작업까지 함께 막는다.
	// 큐가 차면 호출 스레드가 직접 실행한다. 인증번호는 버리는 것보다 늦게라도 나가는 편이 낫다.
	@Bean(name = MAIL_EXECUTOR)
	public Executor mailExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("mail-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();

		return executor;
	}

}
