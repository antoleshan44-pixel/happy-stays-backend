// File: src/main/java/com/eserian/homes/service/AdminService.java
// LOCATION: C:\Users\antol\programming projects\EserianHomes-Clean\backend\src\main\java\com\eserian\homes\service\AdminService.java

package com.eserian.homes.service;

import com.eserian.homes.entity.Admin;
import com.eserian.homes.repository.AdminRepository;
import com.eserian.homes.config.AdminRolesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<Admin> authenticate(String email, String password) {
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);

        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (passwordEncoder.matches(password, admin.getPassword()) &&
                    "active".equals(admin.getStatus())) {
                return adminOpt;
            }
        }
        return Optional.empty();
    }

    public Map<String, Object> getAdminWithPermissions(Admin admin) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", admin.getId());
        result.put("name", admin.getName());
        result.put("email", admin.getEmail());
        result.put("role", admin.getRole());
        result.put("status", admin.getStatus());
        result.put("two_factor_enabled", admin.isTwoFactorEnabled());
        result.put("permissions", admin.getAllPermissions());

        AdminRolesConfig.Role roleDetails = AdminRolesConfig.getRole(admin.getRole());
        if (roleDetails != null) {
            result.put("role_name", roleDetails.getDisplayName());
            result.put("role_level", roleDetails.getLevel());
            result.put("role_color", roleDetails.getColor());
            result.put("role_icon", roleDetails.getIcon());
        }
        return result;
    }

    @Transactional
    public Admin createAdmin(String name, String email, String password, String role) {
        Admin admin = new Admin();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(role);
        admin.setStatus("active");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(admin);
    }

    @Transactional
    public void updateLastLogin(Long adminId, String ipAddress) {
        adminRepository.findById(adminId).ifPresent(admin -> {
            admin.setLastLoginAt(LocalDateTime.now());
            admin.setLastLoginIp(ipAddress);
            adminRepository.save(admin);
        });
    }

    public Optional<Admin> getAdminById(Long id) {
        return adminRepository.findById(id);
    }

    // ============================================
    // ADDED MISSING METHOD - REQUIRED FOR AdminAuthController
    // ============================================
    public Optional<Admin> getAdminByEmail(String email) {
        return adminRepository.findByEmail(email);
    }
    // ============================================

    public Map<String, Object> getAllRoles() {
        Map<String, Object> result = new HashMap<>();
        Map<String, AdminRolesConfig.Role> roles = AdminRolesConfig.getAllRoles();

        for (Map.Entry<String, AdminRolesConfig.Role> entry : roles.entrySet()) {
            AdminRolesConfig.Role role = entry.getValue();
            Map<String, Object> roleInfo = new HashMap<>();
            roleInfo.put("name", role.getName());
            roleInfo.put("display_name", role.getDisplayName());
            roleInfo.put("level", role.getLevel());
            roleInfo.put("description", role.getDescription());
            roleInfo.put("color", role.getColor());
            roleInfo.put("icon", role.getIcon());
            roleInfo.put("permissions_count", role.getPermissions().size());
            result.put(entry.getKey(), roleInfo);
        }
        return result;
    }
}