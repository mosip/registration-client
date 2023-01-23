package io.mosip.registration.test.util.common;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.util.common.BIRBuilder;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ RegistrationAppHealthCheckUtil.class, ApplicationContext.class })
public class BIRBuilderTest {

	@InjectMocks
	private BIRBuilder bIRBuilder;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Test
	public void buildBirRightThumbFingerIndexTest() {
		BiometricsDto biometricsDto = new BiometricsDto();
		biometricsDto.setBioAttribute("rightThumb");
		biometricsDto.setQualityScore(70.0);
		BIR bir = bIRBuilder.buildBIR(biometricsDto);
		Assert.assertNotNull(bir);
		Assert.assertEquals(0, bir.getBdb().length, 0);
		double scoreInBIR = bir.getBdbInfo().getQuality().getScore();
		Assert.assertEquals(biometricsDto.getQualityScore(), scoreInBIR, 0);
	}

	@Test
	public void buildBirRightFingerIndexTest() {
		BiometricsDto biometricsDto = new BiometricsDto();
		biometricsDto.setBioAttribute("rightIndex");
		biometricsDto.setQualityScore(70.0);
		BIR bir = bIRBuilder.buildBIR(biometricsDto);
		Assert.assertNotNull(bir);
		Assert.assertEquals(0, bir.getBdb().length, 0);
		double scoreInBIR = bir.getBdbInfo().getQuality().getScore();
		Assert.assertEquals(biometricsDto.getQualityScore(), scoreInBIR, 0);
	}

	@Test
	public void buildBirLeftFingerIndexTest() {
		BiometricsDto biometricsDto = new BiometricsDto();
		biometricsDto.setBioAttribute("leftIndex");
		biometricsDto.setQualityScore(70.0);
		BIR bir = bIRBuilder.buildBIR(biometricsDto);
		Assert.assertNotNull(bir);
		Assert.assertEquals(0, bir.getBdb().length, 0);
		double scoreInBIR = bir.getBdbInfo().getQuality().getScore();
		Assert.assertEquals(biometricsDto.getQualityScore(), scoreInBIR, 0);
	}

	@Test
	public void buildBirFaceTest() {
		BiometricsDto biometricsDto = new BiometricsDto();
		biometricsDto.setBioAttribute("face");
		biometricsDto.setQualityScore(70.0);
		BIR bir = bIRBuilder.buildBIR(biometricsDto);
		Assert.assertNotNull(bir);
		Assert.assertEquals(0, bir.getBdb().length, 0);
		double scoreInBIR = bir.getBdbInfo().getQuality().getScore();
		Assert.assertEquals(biometricsDto.getQualityScore(), scoreInBIR, 0);
	}

	@Test
	public void buildBirIRISRightTest() {
		BiometricsDto biometricsDto = new BiometricsDto();
		biometricsDto.setBioAttribute("rightEye");
		biometricsDto.setQualityScore(70.0);
		BIR bir = bIRBuilder.buildBIR(biometricsDto);
		Assert.assertNotNull(bir);
		Assert.assertEquals(0, bir.getBdb().length, 0);
		double scoreInBIR = bir.getBdbInfo().getQuality().getScore();
		Assert.assertEquals(biometricsDto.getQualityScore(), scoreInBIR, 0);
	}

	@Test
	public void buildBirIRISLeftTest() {
		BiometricsDto biometricsDto = new BiometricsDto();
		biometricsDto.setBioAttribute("leftEye");
		biometricsDto.setQualityScore(70.0);
		BIR bir = bIRBuilder.buildBIR(biometricsDto);
		Assert.assertNotNull(bir);
		Assert.assertEquals(0, bir.getBdb().length, 0);
		double scoreInBIR = bir.getBdbInfo().getQuality().getScore();
		Assert.assertEquals(biometricsDto.getQualityScore(), scoreInBIR, 0);
	}

	@Test
	public void buildBirTest() throws Throwable, IOException {
		byte[] iso = "slkdalskdjslkajdjadj".getBytes();
		Assert.assertNotNull(bIRBuilder.buildBir("Face", 2, iso, ProcessedLevelType.INTERMEDIATE));
	}
}
