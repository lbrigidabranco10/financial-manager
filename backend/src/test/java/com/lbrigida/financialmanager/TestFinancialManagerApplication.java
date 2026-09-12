package com.lbrigida.financialmanager;

import org.springframework.boot.SpringApplication;

public class TestFinancialManagerApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinancialManagerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
