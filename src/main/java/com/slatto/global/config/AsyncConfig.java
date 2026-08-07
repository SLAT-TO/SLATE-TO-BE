package com.slatto.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

	public static final String MAIL_EXECUTOR = "mailExecutor";

	// 메일 전용 풀이다. 공용 풀을 쓰면 SMTP 지연이 다른 비동기 작업까지 함께 막는다.
	// 큐가 차면 버린다. 인증번호 발송은 공개 엔드포인트라 호출 스레드에서 SMTP 를 태우면
	// 반복 호출만으로 요청 스레드가 묶인다. 발송 실패는 사용자의 재발송으로 갈음하는 정책이라
	// 여기서 버리는 것도 같은 처리다. 예외를 던지면 커밋 후 콜백에서 터지므로 로그만 남긴다.
	@Bean(name = MAIL_EXECUTOR)
	public Executor mailExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("mail-");
		executor.setRejectedExecutionHandler((rejected, poolExecutor) ->
			log.warn("[Mail] 발송 큐가 가득 차 작업을 버렸다. 사용자는 재발송으로 처리한다."));
		executor.initialize();

		return executor;
	}

}
