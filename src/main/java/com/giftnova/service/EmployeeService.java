package com.giftnova.service;

import com.giftnova.dto.CsvUploadResult;
import com.giftnova.model.Employee;
import com.giftnova.repository.EmployeeRepository;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Handles CSV parsing, validation, and saving employees.
 * After a successful upload, triggers event regeneration.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final EventGeneratorService eventGenerator;

    public EmployeeService(EmployeeRepository employeeRepo,
                           EventGeneratorService eventGenerator) {
        this.employeeRepo   = employeeRepo;
        this.eventGenerator = eventGenerator;
    }

    public List<Employee> getEmployees(Long companyId) {
        return employeeRepo.findByCompanyId(companyId);
    }

    /**
     * Parses the uploaded CSV file row by row.
     * Validates name, email, and date formats.
     * If an employee with the same email exists, updates them (no duplicates).
     * After saving all rows, regenerates upcoming events for the company.
     */
    public CsvUploadResult upload(Long companyId, MultipartFile file) throws Exception {
        CsvUploadResult result = new CsvUploadResult();

        // Parse CSV with header row
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            int row = 2; // row 1 is the header
            for (CSVRecord record : parser) {
                Employee emp = parseRow(companyId, record, row, result);
                if (emp != null) {
                    // Update existing employee or save new one
                    Optional<Employee> existing =
                            employeeRepo.findByCompanyIdAndEmail(companyId, emp.getEmail());
                    if (existing.isPresent()) {
                        copyFields(emp, existing.get());
                        employeeRepo.save(existing.get());
                    } else {
                        employeeRepo.save(emp);
                        result.incrementSuccess();
                    }
                }
                row++;
            }
        }

        // Regenerate upcoming events from the full updated employee list
        eventGenerator.regenerateEvents(companyId, employeeRepo.findByCompanyId(companyId));
        return result;
    }

    // Parses one CSV row into an Employee; adds to result.errors and returns null on failure
    private Employee parseRow(Long companyId, CSVRecord record, int row, CsvUploadResult result) {
        String name  = get(record, "name");
        String email = get(record, "email");

        // Required field validation
        if (name == null || name.isBlank()) {
            result.addError("Row " + row + ": Name is required");
            return null;
        }
        if (email == null || email.isBlank()) {
            result.addError("Row " + row + ": Email is required");
            return null;
        }
        if (!email.contains("@")) {
            result.addError("Row " + row + ": Invalid email — '" + email + "'");
            return null;
        }

        Employee emp = new Employee();
        emp.setCompanyId(companyId);
        emp.setName(name);
        emp.setEmail(email);
        emp.setDepartment(get(record, "department"));
        emp.setManager(get(record, "manager"));
        emp.setManagerEmail(get(record, "manager_email"));
        emp.setCountry(get(record, "country"));

        // Date fields — must be yyyy-MM-dd format
        emp.setStartDate(parseDate(record, "start_date", row, result));
        emp.setBirthday(parseDate(record, "birthday", row, result));

        // Null date parse errors are already added to result; still save the employee
        return emp;
    }

    // Parses a date field; adds an error and returns null if the format is wrong
    private LocalDate parseDate(CSVRecord record, String field, int row, CsvUploadResult result) {
        String val = get(record, field);
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val);
        } catch (DateTimeParseException e) {
            result.addError("Row " + row + ": Invalid " + field + " '" + val + "' — use yyyy-MM-dd");
            return null;
        }
    }

    // Safely reads a CSV column; returns null if the column doesn't exist
    private String get(CSVRecord record, String col) {
        try { return record.get(col); } catch (IllegalArgumentException e) { return null; }
    }

    // Copies all mutable fields from parsed CSV into existing DB entity (for updates)
    private void copyFields(Employee src, Employee dest) {
        dest.setName(src.getName());
        dest.setDepartment(src.getDepartment());
        dest.setManager(src.getManager());
        dest.setManagerEmail(src.getManagerEmail());
        dest.setCountry(src.getCountry());
        dest.setStartDate(src.getStartDate());
        dest.setBirthday(src.getBirthday());
    }
}
