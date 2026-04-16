package com.qar.securitysystem;

import com.qar.securitysystem.dto.EncryptedFileUploadRequest;
import com.qar.securitysystem.dto.FileRecordResponse;
import com.qar.securitysystem.dto.FlightXlsxPreviewResponse;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.service.FileService;
import com.qar.securitysystem.service.FlightXlsxService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@SpringBootTest
class SecuritysystemApplicationTests {
	@Autowired
	private FileService fileService;

	@Autowired
	private FlightXlsxService flightXlsxService;

	@Test
	void contextLoads() {
	}

	@Test
	void flightPreviewCanReadPlaintextFromEncryptedStoredFile() throws Exception {
		byte[] xlsxBytes;
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			var sheet = workbook.createSheet("FlightData");
			var header = sheet.createRow(0);
			header.createCell(0).setCellValue("航班号");
			header.createCell(1).setCellValue("时间");
			var row = sheet.createRow(1);
			row.createCell(0).setCellValue("CA1234");
			row.createCell(1).setCellValue("2026-04-16T08:00:00Z");
			workbook.write(out);
			xlsxBytes = out.toByteArray();
		}

		UserEntity owner = new UserEntity();
		owner.setId("test-user");
		owner.setPersonId("test-person");

		EncryptedFileUploadRequest req = new EncryptedFileUploadRequest();
		req.setEncryptedData(Base64.getEncoder().encodeToString(xlsxBytes));
		req.setOriginalName("flight-preview.xlsx");
		req.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		req.setSizeBytes((long) xlsxBytes.length);
		req.setPolicy("role:admin");

		FileRecordResponse saved = fileService.storeEncrypted(owner, req);
		FlightXlsxPreviewResponse preview = flightXlsxService.previewFile(saved.getId(), 20);

		Assertions.assertNotNull(preview);
		Assertions.assertFalse(preview.getSheets().isEmpty());
		Assertions.assertEquals("CA1234", String.valueOf(preview.getSheets().get(0).getRows().get(0).get("航班号")));
	}

}
