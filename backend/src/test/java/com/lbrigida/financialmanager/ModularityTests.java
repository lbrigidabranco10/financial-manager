package com.lbrigida.financialmanager;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

	@Test
	void verifiesModuleBoundaries() {
		ApplicationModules.of(FinancialManagerApplication.class).verify();
	}

}
