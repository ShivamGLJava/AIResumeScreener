package com.resumescreener.resumescreener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Resume Screener Application Tests")
@SpringBootTest
@ActiveProfiles("test")
class ResumescreenerApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("Application context should load successfully")
	void contextLoads() {
		assertNotNull(applicationContext, "Application context should not be null");
	}

	@Test
	@DisplayName("Resume controller bean should be created")
	void resumeControllerBeanShouldExist() {
		assertNotNull(
			applicationContext.getBean("resumeController"),
			"Resume controller bean should exist in the application context"
		);
	}

}
