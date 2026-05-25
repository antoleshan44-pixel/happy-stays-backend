// File: src/main/java/com/eserian/homes/config/AdminRolesConfig.java
// LOCATION: BACKEND - Spring Boot Configuration

package com.eserian.homes.config;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;  // ← CHANGE THIS (was javax.annotation)
import java.util.*;

@Component
public class AdminRolesConfig {

    private static final Map<String, Role> ROLES = new HashMap<>();

    // Role class definition
    public static class Role {
        private String name;
        private String displayName;
        private int level;
        private Set<String> permissions;
        private String description;
        private String color;
        private String icon;

        public Role(String name, String displayName, int level, Set<String> permissions,
                    String description, String color, String icon) {
            this.name = name;
            this.displayName = displayName;
            this.level = level;
            this.permissions = permissions;
            this.description = description;
            this.color = color;
            this.icon = icon;
        }

        // Getters
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }
        public Set<String> getPermissions() { return permissions; }
        public String getDescription() { return description; }
        public String getColor() { return color; }
        public String getIcon() { return icon; }
    }

    @PostConstruct
    public void init() {
        // Super Admin Role
        ROLES.put("super_admin", new Role(
                "super_admin",
                "Super Administrator",
                100,
                new HashSet<>(Collections.singletonList("*")),
                "Full system access, can manage other admins, system settings, and all platform features",
                "#dc3545",
                "fas fa-crown"
        ));

        // Property Moderator Role
        Set<String> propertyPermissions = new HashSet<>(Arrays.asList(
                "properties.view", "properties.approve", "properties.reject",
                "properties.suspend", "properties.archive", "properties.edit",
                "photos.view", "photos.moderate", "photos.delete",
                "videos.view", "videos.moderate", "videos.delete",
                "reviews.view", "reviews.moderate", "reviews.delete"
        ));
        ROLES.put("property_moderator", new Role(
                "property_moderator",
                "Property Moderator",
                70,
                propertyPermissions,
                "Manages property listings, approves/rejects properties, moderates photos, videos, and reviews",
                "#007bff",
                "fas fa-building"
        ));

        // Finance Admin Role
        Set<String> financePermissions = new HashSet<>(Arrays.asList(
                "payments.view", "payments.process", "payments.process_refunds",
                "payouts.view", "payouts.approve", "payouts.process",
                "commissions.view", "commissions.configure",
                "transactions.view", "reports.financial", "reports.tax",
                "invoices.view", "invoices.generate", "tax.configure"
        ));
        ROLES.put("finance_admin", new Role(
                "finance_admin",
                "Finance Administrator",
                80,
                financePermissions,
                "Manages payments, payouts, commissions, financial reports, and tax configurations",
                "#28a745",
                "fas fa-money-bill-wave"
        ));

        // Support Admin Role
        Set<String> supportPermissions = new HashSet<>(Arrays.asList(
                "users.view", "users.suspend", "users.activate",
                "bookings.view", "bookings.modify", "bookings.cancel",
                "disputes.view", "disputes.manage", "disputes.resolve",
                "tickets.view", "tickets.manage", "messages.view",
                "kyc.view", "kyc.verify"
        ));
        ROLES.put("support_admin", new Role(
                "support_admin",
                "Support Administrator",
                60,
                supportPermissions,
                "Handles user support, disputes, tickets, booking modifications, and KYC verification",
                "#17a2b8",
                "fas fa-headset"
        ));

        // Fraud Analyst Role
        Set<String> fraudPermissions = new HashSet<>(Arrays.asList(
                "fraud.view", "fraud.investigate", "fraud.flag",
                "blacklist.ip.view", "blacklist.ip.add", "blacklist.ip.remove",
                "users.view", "users.verify", "transactions.view",
                "reports.fraud", "alerts.view"
        ));
        ROLES.put("fraud_analyst", new Role(
                "fraud_analyst",
                "Fraud Analyst",
                75,
                fraudPermissions,
                "Investigates fraud, suspicious activity, manages blacklists, and monitors risk",
                "#fd7e14",
                "fas fa-shield-alt"
        ));

        // Content Moderator Role
        Set<String> contentPermissions = new HashSet<>(Arrays.asList(
                "reviews.view", "reviews.moderate", "reviews.delete",
                "photos.view", "photos.moderate", "photos.delete",
                "videos.view", "videos.moderate", "videos.delete",
                "messages.view", "messages.moderate",
                "cms.view", "cms.edit", "announcements.create",
                "faq.view", "faq.edit"
        ));
        ROLES.put("content_moderator", new Role(
                "content_moderator",
                "Content Moderator",
                50,
                contentPermissions,
                "Moderates user-generated content including reviews, photos, videos, messages, and manages CMS",
                "#6c757d",
                "fas fa-flag"
        ));

        // Marketing Admin Role
        Set<String> marketingPermissions = new HashSet<>(Arrays.asList(
                "promotions.view", "promotions.create", "promotions.edit",
                "campaigns.view", "campaigns.create", "campaigns.analytics",
                "newsletters.view", "newsletters.create", "newsletters.send",
                "analytics.view", "featured.listings.add", "discounts.create",
                "seo.view", "homepage.edit"
        ));
        ROLES.put("marketing_admin", new Role(
                "marketing_admin",
                "Marketing Administrator",
                65,
                marketingPermissions,
                "Manages promotions, marketing campaigns, newsletters, featured listings, and SEO",
                "#e83e8c",
                "fas fa-megaphone"
        ));
    }

    public static Role getRole(String roleName) {
        return ROLES.get(roleName);
    }

    public static Set<String> getPermissionsForRole(String roleName) {
        Role role = ROLES.get(roleName);
        return role != null ? role.getPermissions() : new HashSet<>();
    }

    public static boolean hasPermission(String roleName, String permission) {
        Role role = ROLES.get(roleName);
        if (role == null) return false;
        Set<String> permissions = role.getPermissions();
        return permissions.contains("*") || permissions.contains(permission);
    }

    public static Map<String, Role> getAllRoles() {
        return Collections.unmodifiableMap(ROLES);
    }

    public static boolean canManageRole(String adminRole, String targetRole) {
        if ("super_admin".equals(adminRole)) return true;
        return false;
    }
}